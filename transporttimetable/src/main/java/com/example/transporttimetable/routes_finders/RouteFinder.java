package com.example.transporttimetable.routes_finders;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.transporttimetable.models.FoundRoute;
import com.example.transporttimetable.models.Route;
import com.example.transporttimetable.models.Station;
import com.example.transporttimetable.models.Step;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Geo;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.PedestrianRouter;
import com.yandex.mapkit.transport.masstransit.Session;
import com.yandex.mapkit.transport.masstransit.Summary;
import com.yandex.mapkit.transport.masstransit.SummarySession;
import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.runtime.Error;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

public class RouteFinder {

    private static final int MAX_TRANSFER = 2;
    private static final int MAX_ROUTES = 5;
    private static final int MAX_WALK_TIME_MINUTES = 3;
    private static final String TAG = "RouteFinder";
    private Context context;
    private List<Station> allStations;
    private List<Route> allRoutes;
    private OrtEnvironment env;
    private OrtSession session;
    public RouteFinder(Context context, List<Station> allStations, List<Route> allRoutes) {
        this.context = context;
        this.allStations = allStations;
        this.allRoutes = allRoutes;
        try {
            env = OrtEnvironment.getEnvironment();
            session = env.createSession(copyAssetToFile(context, "bus_time_predictor.onnx").getAbsolutePath(),
                    new OrtSession.SessionOptions());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при инициализации ONNX модели", e);
        }
    }
    private File copyAssetToFile(Context context, String filename) throws Exception {
        File outFile = new File(context.getFilesDir(), filename);
        if (!outFile.exists()) {
            InputStream is = context.getAssets().open(filename);
            FileOutputStream os = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            is.close();
            os.close();
        }
        return outFile;
    }

    public List<Integer> predictTimes(List<Float> baseTimes, List<Integer> distances, int dayOfWeek, int hour,
                                      int isPeak, int weather, int traffic, int season, float targetTotalTime) {
        try {
            int n = baseTimes.size();
            float[][] inputData = new float[n][8];

            Log.d(TAG, "===== ВХОДНЫЕ ДАННЫЕ В МОДЕЛЬ =====");
            for (int i = 0; i < n; i++) {
                inputData[i][0] = season;
                inputData[i][1] = dayOfWeek;
                inputData[i][2] = hour;
                inputData[i][3] = baseTimes.get(i);
                inputData[i][4] = traffic;
                inputData[i][5] = weather;
                inputData[i][6] = isPeak;
                inputData[i][7] = distances.get(i); // добавлено расстояние

                Log.d(TAG, String.format("Input #%d: season=%d, day=%d, hour=%d, base=%.2f, traffic=%d, weather=%d, isPeak=%d, dist=%d",
                        i, season, dayOfWeek, hour, baseTimes.get(i), traffic, weather, isPeak, distances.get(i)));
            }

            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputData);
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(session.getInputNames().iterator().next(), inputTensor);

            OrtSession.Result results = session.run(inputs);
            float[][] output = (float[][]) results.get(0).getValue();

            Log.d(TAG, "===== ПОЛУЧЕННОЕ ВРЕМЯ ОТ МОДЕЛИ =====");
            List<Float> rawTimes = new ArrayList<>();
            float predictedTotal = 0f;

            for (int i = 0; i < n; i++) {
                float t = output[i][0];
                rawTimes.add(t);
                predictedTotal += t;
                Log.d(TAG, String.format("Segment #%d: predictedTime=%.2f", i, t));
            }

            List<Integer> finalTimes = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int finalTime = Math.round(rawTimes.get(i));
                finalTimes.add(finalTime);
                Log.d(TAG, String.format("Финальное время #%d: %.2f = %d", i, rawTimes.get(i), finalTime));
            }

            return finalTimes;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при предсказании времени", e);
            return Collections.emptyList();
        }
    }

    public void findRoutes(Point pointA, Point pointB, RouteCallback callback) {
        Log.e(TAG, "findRoutes: Start. PointA: " + formatPoint(pointA) + ", PointB: " + formatPoint(pointB));

        findNearestStations(pointA, startStations -> {
            Log.e(TAG, "findRoutes: Nearest start stations found: " + startStations.size());

            findNearestStations(pointB, endStations -> {
                Log.e(TAG, "findRoutes: Nearest end stations found: " + endStations.size());

                if (startStations.isEmpty() || endStations.isEmpty()) {
                    Log.e(TAG, "findRoutes: No start or end stations found. Returning empty route list.");
                    callback.onRoutesFound(new ArrayList<>());
                    return;
                }

                Map<Station, List<Edge>> graph = buildGraph();
                Log.e(TAG, "findRoutes: Graph built with " + graph.size() + " nodes.");

                List<FoundRoute> foundRoutes = new ArrayList<>();
                findRoutesRecursive(startStations, endStations, pointA, pointB, graph, foundRoutes, 0, callback);
            });
        });
    }

    private void findRoutesRecursive(List<Station> startStations, List<Station> endStations, Point pointA, Point pointB,
                                     Map<Station, List<Edge>> graph, List<FoundRoute> foundRoutes,
                                     int index, RouteCallback callback) {
        Log.e(TAG, "findRoutesRecursive: index=" + index + ", foundRoutes size=" + foundRoutes.size());

        if (index >= startStations.size() || foundRoutes.size() >= MAX_ROUTES) {
            Log.e(TAG, "findRoutesRecursive: Recursion done. Total found routes: " + foundRoutes.size());
            callback.onRoutesFound(foundRoutes);
            return;
        }

        Station start = startStations.get(index);
        Log.e(TAG, "findRoutesRecursive: Trying from start station id=" + start.getId());

        estimateWalkTime(pointA, start.getCoordinates(), walkTimeStart -> {
            Log.e(TAG, "findRoutesRecursive: Walk time from start=" + walkTimeStart + " minutes");

            if (walkTimeStart > MAX_WALK_TIME_MINUTES) {
                Log.e(TAG, "findRoutesRecursive: Walk time exceeds maximum (" + MAX_WALK_TIME_MINUTES + " minutes), skipping station id=" + start.getId());
                findRoutesRecursive(startStations, endStations, pointA, pointB, graph, foundRoutes, index + 1, callback);
                return;
            }

            dijkstra(graph, start, endStations, walkTimeStart, pointB, route -> {
                if (route != null) {
                    Log.e(TAG, "findRoutesRecursive: Found a route with total time=" + route.getTotalTime());
                    foundRoutes.add(route);
                } else {
                    Log.e(TAG, "findRoutesRecursive: No route found from start station id=" + start.getId());
                }
                findRoutesRecursive(startStations, endStations, pointA, pointB, graph, foundRoutes, index + 1, callback);
            });
        });
    }

    private void dijkstra(Map<Station, List<Edge>> graph, Station startStation, List<Station> endStations,
                          int walkTimeStart, Point endPoint, DijkstraResultCallback callback) {
        Log.e(TAG, "dijkstra: Start from station id=" + startStation.getId() + ", walkTimeStart=" + walkTimeStart);

        PriorityQueue<PathNode> queue = new PriorityQueue<>(Comparator.comparingInt(n -> n.totalTime));
        Map<Integer, Integer> visited = new HashMap<>();

        List<RouteStepInternal> initialSteps = new ArrayList<>();
        initialSteps.add(new RouteStepInternal(RouteStepInternal.Type.WALK, null, new ArrayList<>()));

        queue.add(new PathNode(startStation, initialSteps, walkTimeStart, 0));

        processQueue(queue, visited, graph, endStations, endPoint, callback);
    }

    private void processQueue(PriorityQueue<PathNode> queue, Map<Integer, Integer> visited,
                              Map<Station, List<Edge>> graph, List<Station> endStations,
                              Point endPoint, DijkstraResultCallback callback) {
        Log.e(TAG, "processQueue: Queue size=" + queue.size());

        if (queue.isEmpty()) {
            Log.e(TAG, "processQueue: Queue is empty, no route found");
            callback.onResult(null);
            return;
        }

        PathNode current = queue.poll();
        Log.e(TAG, "processQueue: Processing station id=" + current.station.getId() + ", totalTime=" + current.totalTime);

        if (visited.containsKey(current.station.getId()) && visited.get(current.station.getId()) <= current.totalTime) {
            Log.e(TAG, "processQueue: Already visited station id=" + current.station.getId() + " with better time, skipping");
            processQueue(queue, visited, graph, endStations, endPoint, callback);
            return;
        }

        visited.put(current.station.getId(), current.totalTime);

        boolean isEndStation = endStations.stream().anyMatch(end -> end.getId() == current.station.getId());
        if (isEndStation) {
            Log.e(TAG, "processQueue: Reached end station id=" + current.station.getId());
            estimateWalkTime(current.station.getCoordinates(), endPoint, walkTimeEnd -> {
                Log.e(TAG, "processQueue: Estimated walk time to end point=" + walkTimeEnd + " minutes");

                if (walkTimeEnd <= MAX_WALK_TIME_MINUTES) {
                    List<Step> finalSteps = convertToSteps(current.steps);
                    finalSteps.add(new Step.Walk());
                    FoundRoute foundRoute = new FoundRoute(finalSteps, current.totalTime + walkTimeEnd);
                    Log.e(TAG, "processQueue: Found a valid route with total time=" + foundRoute.getTotalTime());
                    callback.onResult(foundRoute);
                } else {
                    Log.w(TAG, "processQueue: Walk time to end point exceeds maximum (" + MAX_WALK_TIME_MINUTES + " minutes), skipping");
                    callback.onResult(null);
                }
            });
            return;
        }

        List<Edge> edges = graph.getOrDefault(current.station, new ArrayList<>());

        if (edges.isEmpty()) {
            Log.e(TAG, "processQueue: No outgoing edges from station id=" + current.station.getId());
        }

        // Сохраняем последний автобус для правильной проверки
        String lastBusNumber = null;
        if (!current.steps.isEmpty()) {
            RouteStepInternal lastStep = current.steps.get(current.steps.size() - 1);
            if (lastStep.type == RouteStepInternal.Type.BUS) {
                lastBusNumber = lastStep.busNumber;
            }
        }

        for (Edge edge : edges) {
            Log.e(TAG, "processQueue: Exploring edge from station " + current.station.getId() + " to station " + edge.to.getId() + " by bus " + edge.busNumber);

            List<RouteStepInternal> newSteps = new ArrayList<>(current.steps);
            int newTransfers = current.transfers;
            boolean isSameBus = lastBusNumber != null && lastBusNumber.equals(String.valueOf(edge.busNumber));

            if (isSameBus) {
                // Едем дальше на том же автобусе — просто добавляем остановку
                RouteStepInternal lastStep = newSteps.get(newSteps.size() - 1);
                lastStep.stations.add(edge.to.getId());
            } else {
                // ПЕРЕСАДКА только если пересадка возможна
                if (lastBusNumber != null) {
                    // Добавляем шаг пересадки
                    newSteps.add(new RouteStepInternal(RouteStepInternal.Type.TRANSFER, null, null));
                }
                // И новый автобусный шаг
                RouteStepInternal newBusStep = new RouteStepInternal(RouteStepInternal.Type.BUS, String.valueOf(edge.busNumber), new ArrayList<>());
                newBusStep.stations.add(edge.to.getId());
                newSteps.add(newBusStep);

                newTransfers++;
                if (newTransfers > MAX_TRANSFER) {
                    Log.e(TAG, "processQueue: Too many transfers (" + newTransfers + "), skipping edge to station id=" + edge.to.getId());
                    continue;
                }
            }
            queue.clear();
            // Теперь добавляем в очередь
            queue.add(new PathNode(edge.to, newSteps, current.totalTime + edge.time, newTransfers));
        }

        processQueue(queue, visited, graph, endStations, endPoint, callback);
    }

    private String formatPoint(Point point) {
        return "Latitude: " + point.getLatitude() + ", Longitude: " + point.getLongitude();
    }

    private List<Step> convertToSteps(List<RouteStepInternal> internalSteps) {
        Log.e("RouteFinder", "convertToSteps: internalSteps size=" + internalSteps.size());
        List<Step> steps = new ArrayList<>();
        for (RouteStepInternal internalStep : internalSteps) {
            switch (internalStep.type) {
                case WALK:
                    steps.add(new Step.Walk());
                    break;
                case BUS:
                    steps.add(new Step.Bus(internalStep.busNumber, internalStep.stations));
                    break;
                case TRANSFER:
                    steps.add(new Step.Transfer("Пересадка"));
                    break;
            }
        }
        Log.e("RouteFinder", "convertToSteps: steps size=" + steps.size());
        return steps;
    }
    private List<Float> parseTimeIntervals(String timeStr) {
        List<Float> times = new ArrayList<>();
        if (timeStr == null || timeStr.isEmpty()) return times;

        String[] parts = timeStr.split(",");
        for (String part : parts) {
            if (!part.isEmpty()) {
                try {
                    times.add(Float.parseFloat(part.trim()));
                } catch (NumberFormatException e) {
                    Log.e("RouteFinder", "Ошибка парсинга времени: " + part);
                }
            }
        }
        return times;
    }
    private Map<Station, List<Edge>> buildGraph() {
        Log.e("RouteFinder", "buildGraph: Start");
        Map<Station, List<Edge>> graph = new HashMap<>();

        // параметры для модели
        int dayOfWeek = getCurrentDayOfWeek(); // 0 - пн, 6 - вс
        int hour = getCurrentHour();
        int isPeak = isPeakHour(hour) ? 1 : 0;
        int weather = getWeatherCode(); // заглушка или из API
        int traffic = getTrafficLevel(); // заглушка или API
        int season = getSeason();

        for (Route route : allRoutes) {
            List<Integer> stationIds = parseStationIds(route.getStop());
            List<Float> baseTimes = parseTimeIntervals(route.getTime());
            List<Integer> distances = parseStationIds(route.getDistance());
            if (baseTimes.size() != stationIds.size() - 1) {
                Log.e("RouteFinder", "Пропуск маршрута " + route.getBus() + ": некорректные данные " + stationIds.size() + "  Время: " + baseTimes.size() + " Остановок: " + stationIds.size());
                continue;
            }

            float totalTime = baseTimes.stream().reduce(0f, Float::sum);
            // получить прогнозируемое время
            List<Integer> predictedTimes = predictTimes(baseTimes,distances, dayOfWeek, hour, isPeak, weather, traffic, season, totalTime);

            for (int i = 0; i < stationIds.size() - 1; i++) {
                Station fromStation = findStationById(stationIds.get(i));
                Station toStation = findStationById(stationIds.get(i + 1));

                if (fromStation == null || toStation == null) continue;
                float originalTime = baseTimes.get(i);

                int predictedTime  = predictedTimes.get(i);
                Log.d("RouteFinder", "Маршрут " + route.getBus() + ": " +
                        fromStation.getName() + " → " + toStation.getName() +
                        " | базовое: " + originalTime + " мин → предсказано: " + predictedTime + " мин");
                graph.computeIfAbsent(fromStation, k -> new ArrayList<>())
                        .add(new Edge(toStation, predictedTime, route.getBus()));
            }
        }

        // Красиво печатаем построенный граф
        Log.e("RouteFinder", "===== Граф маршрутов построен =====");
        for (Map.Entry<Station, List<Edge>> entry : graph.entrySet()) {
            Station fromStation = entry.getKey();
            List<Edge> edges = entry.getValue();

            StringBuilder sb = new StringBuilder();
            sb.append("Станция ID=").append(fromStation.getId()).append(" ведет к: ");

            for (Edge edge : edges) {
                sb.append("\n    -> Станция ID=").append(edge.to.getId())
                        .append(" (Автобус ").append(edge.busNumber)
                        .append(", время ").append(edge.time).append(" мин)");
            }

            Log.e("RouteFinder", sb.toString());
        }

        Log.e("RouteFinder", "===== Конец графа =====");

        return graph;
    }
    private int getCurrentDayOfWeek() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK); // Sunday=1, Saturday=7
        return (day + 5) % 7 + 1; // Преобразуем: Sunday=6, Monday=0
    }

    private int getCurrentHour() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    private boolean isPeakHour(int hour) {
        return (hour >= 7 && hour <= 9) || (hour >= 12 && hour <= 13)|| (hour >= 17 && hour <= 19);
    }

    private int getWeatherCode() {
        return 0; // заглушка, позже можно подставить код из погодного API
    }

    private int getTrafficLevel() {
        return 1; // заглушка — можно внедрить позже
    }

    private int getSeason() {
        int month = Calendar.getInstance().get(Calendar.MONTH); // 0=Jan
        if (month >= 2 && month <= 4) return 2; // Весна
        if (month >= 5 && month <= 7) return 3; // Лето
        if (month >= 8 && month <= 10) return 4; // Осень
        return 1; // Зима
    }
    private List<Integer> parseStationIds(String stationsString) {
        List<Integer> stationIds = new ArrayList<>();
        String[] parts = stationsString.split(",");
        for (String part : parts) {
            if (!part.isEmpty()) {
                stationIds.add(Integer.parseInt(part));
            }
        }
        return stationIds;
    }

    private Station findStationById(int id) {
        for (Station station : allStations) {
            if (station.getId() == id) {
                return station;
            }
        }
        return null;
    }

    private void findNearestStations(Point location, NearestStationsResult callback) {
        Log.e(TAG, "findNearestStations: START for " + formatPoint(location));
        if (allStations.isEmpty()) {
            Log.e(TAG, "findNearestStations: no stations at all → return empty");
            callback.onStationsFound(Collections.emptyList());
            return;
        }

        List<Station> withinRadius = new ArrayList<>();
        for (Station st : allStations) {
            double dist = Geo.distance(location,
                    st.getCoordinates());
            if (dist <= 250) {
                withinRadius.add(st);
                Log.e(TAG, "findNearestStations: → added station #" + st.getId());
            }
        }

        if (withinRadius.isEmpty()) {
            Log.e(TAG, "findNearestStations: no stations within 100m → return empty");
            callback.onStationsFound(Collections.emptyList());
            return;
        }

        List<Station> nearest = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(withinRadius.size());

        for (Station st : withinRadius) {
            estimateWalkTime(location, st.getCoordinates(), minutes -> {
                if (minutes <= MAX_WALK_TIME_MINUTES) {
                    synchronized (nearest) {
                        nearest.add(st);
                    }
                }
                latch.countDown();
            });
        }

        new Thread(() -> {
            try {
                latch.await();
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onStationsFound(new ArrayList<>(nearest))
                );
            } catch (InterruptedException e) {
                Log.e(TAG, "findNearestStations: interrupted", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        callback.onStationsFound(Collections.emptyList())
                );
            }
        }).start();
    }

    private void estimateWalkTime(Point from, Point to, EstimateCallback callback) {
        //Log.e(TAG, "estimateWalkTime: Request pedestrian route from "+ formatPoint(from) + " to " + formatPoint(to));

        // Заодно: MapKitFactory.initialize() делаем один раз в Application.onCreate()
        PedestrianRouter router = TransportFactory.getInstance().createPedestrianRouter();

        List<RequestPoint> pts = Arrays.asList(
                new RequestPoint(from, RequestPointType.WAYPOINT, null),
                new RequestPoint(to,   RequestPointType.WAYPOINT, null)
        );

        TimeOptions timeOptions = new TimeOptions(); // оставляем пустые — дефолтные

        router.requestRoutes(pts, timeOptions, new Session.RouteListener() {
            @Override
            public void onMasstransitRoutes(@NonNull List<com.yandex.mapkit.transport.masstransit.Route> routes) {
                //Log.e(TAG, "estimateWalkTime:onMasstransitRoutes: got " + routes.size() + " routes");
                if (routes.isEmpty()) {
                    callback.onEstimated(Integer.MAX_VALUE);
                } else {
                    int sec = (int)routes.get(0).getMetadata().getWeight().getTime().getValue();
                    int min = sec / 60;
                    //Log.e(TAG, "estimateWalkTime: best route time = " + min + " min");
                    callback.onEstimated(min);
                }
            }
            @Override
            public void onMasstransitRoutesError(@NonNull Error error) {
                Log.e(TAG, "estimateWalkTime:onError: " + error);
                callback.onEstimated(Integer.MAX_VALUE);
            }
        });
    }


    // Внутренние классы
    private static class Edge {
        Station to;
        int time;
        int busNumber;

        Edge(Station to, int time, int busNumber) {
            this.to = to;
            this.time = time;
            this.busNumber = busNumber;
        }
    }

    private static class PathNode {
        Station station;
        List<RouteStepInternal> steps;
        int totalTime;
        int transfers;

        PathNode(Station station, List<RouteStepInternal> steps, int totalTime, int transfers) {
            this.station = station;
            this.steps = steps;
            this.totalTime = totalTime;
            this.transfers = transfers;
        }
    }

    private static class RouteStepInternal {
        enum Type { WALK, BUS, TRANSFER }

        Type type;
        String busNumber;
        List<Integer> stations;

        RouteStepInternal(Type type, String busNumber, List<Integer> stations) {
            this.type = type;
            this.busNumber = busNumber;
            this.stations = stations;
        }
    }

    public interface RouteCallback {
        void onRoutesFound(List<FoundRoute> routes);
    }

    private interface NearestStationsResult {
        void onStationsFound(List<Station> stations);
    }

    private interface DijkstraResultCallback {
        void onResult(FoundRoute route);
    }

    private interface EstimateCallback {
        void onEstimated(int minutes);
    }
}