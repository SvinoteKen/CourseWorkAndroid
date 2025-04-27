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
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.PedestrianRouter;
import com.yandex.mapkit.transport.masstransit.Session;
import com.yandex.mapkit.transport.masstransit.Summary;
import com.yandex.mapkit.transport.masstransit.SummarySession;
import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class RouteFinder {

    private static final int MAX_TRANSFER = 2;
    private static final int MAX_ROUTES = 5;
    private static final int MAX_WALK_TIME_MINUTES = 3;
    private static final String TAG = "RouteFinder";
    private Context context;
    private List<Station> allStations;
    private List<Route> allRoutes;

    public RouteFinder(Context context, List<Station> allStations, List<Route> allRoutes) {
        this.context = context;
        this.allStations = allStations;
        this.allRoutes = allRoutes;
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

    private Map<Station, List<Edge>> buildGraph() {
        Log.e("RouteFinder", "buildGraph: Start");
        Map<Station, List<Edge>> graph = new HashMap<>();

        for (Route route : allRoutes) {
            List<Integer> stationIds = parseStationIds(route.getStop());
            Log.e("RouteFinder", "buildGraph: Route bus=" + route.getBus() + ", stations=" + stationIds);

            for (int i = 0; i < stationIds.size() - 1; i++) {
                Station fromStation = findStationById(stationIds.get(i));
                Station toStation = findStationById(stationIds.get(i + 1));

                if (fromStation == null || toStation == null) continue;

                int time = 2; // всегда 2 минуты между соседними остановками
                graph.computeIfAbsent(fromStation, k -> new ArrayList<>())
                        .add(new Edge(toStation, time, route.getBus()));
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

        List<Station> nearest = new ArrayList<>();
        // Начинаем с 0-й станции
        processNextStation(0, location, nearest, callback);
    }

    private void processNextStation(int index,
                                    Point location,
                                    List<Station> nearest,
                                    NearestStationsResult callback) {
        if (index >= allStations.size()) {
            // Все станции обработаны
            //Log.e(TAG, "findNearestStations: DONE, found " + nearest.size());
            callback.onStationsFound(new ArrayList<>(nearest));
            return;
        }

        Station st = allStations.get(index);
        //Log.e(TAG, "findNearestStations: checking station #" + st.getId()+ " \"" + st.getName() + "\" at " + formatPoint(st.getCoordinates()));

        // Оцениваем пеший путь к этой станции
        estimateWalkTime(location, st.getCoordinates(), minutes -> {
            //Log.e(TAG, "findNearestStations: station #" + st.getId()+ " walkTime=" + minutes + " min");
            if (minutes <= MAX_WALK_TIME_MINUTES) {
                nearest.add(st);
                Log.e(TAG, "findNearestStations: → added station #" + st.getId());
            }
            // Немного подождём (опционально, чтобы не давить на API)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Рекурсивно переходим к следующей станции
                processNextStation(index + 1, location, nearest, callback);
            }, 200); // задержка 200 мс между запросами
        });
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