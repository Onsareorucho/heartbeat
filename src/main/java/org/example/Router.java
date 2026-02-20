package org.example;

import java.net.*;
import java.util.concurrent.*;
import org.json.JSONObject;

public class Router {
    // Service registry - thread-safe
    private static ConcurrentHashMap<String, ServiceInfo> activeServices = new ConcurrentHashMap<>();

    // Service info class
    static class ServiceInfo {
        String serviceName;
        String ipAddress;
        int port;
        long lastHeartbeat;

        ServiceInfo(String name, String ip, int port) {
            this.serviceName = name;
            this.ipAddress = ip;
            this.port = port;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        void updateHeartbeat() {
            this.lastHeartbeat = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format("%s running @ socket %s:%d (last seen: %dms ago)",
                    serviceName, ipAddress, port,
                    System.currentTimeMillis() - lastHeartbeat);
        }
    }

    // UDP Heartbeat Listener Thread (based on UDPServer2 pattern)
    static class HeartbeatListener extends Thread {
        private DatagramSocket socket;
        private final int UDP_PORT = 9000;

        public HeartbeatListener() {
            try {
                socket = new DatagramSocket(UDP_PORT);
                System.out.println("Heartbeat listener started on UDP port " + UDP_PORT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] incomingData = new byte[1024];

            while (true) {
                try {
                    // Create and receive packet (from UDPServer2 pattern)
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    socket.receive(incomingPacket);

                    // Extract message
                    String message = new String(incomingPacket.getData(), 0, incomingPacket.getLength());

                    // Get sender's IP address (KEY: extract from packet!)
                    String senderIP = incomingPacket.getAddress().getHostAddress();

                    // Parse JSON heartbeat
                    JSONObject json = new JSONObject(message);
                    String type = json.getString("type");

                    if (type.equals("HEARTBEAT")) {
                        String serviceName = json.getString("service");
                        int servicePort = json.getInt("port");

                        // Update or add service to registry
                        if (activeServices.containsKey(serviceName)) {
                            // Update existing service
                            activeServices.get(serviceName).updateHeartbeat();
                            System.out.println("Updated: " + serviceName + " from " + senderIP);
                        } else {
                            // Add new service
                            ServiceInfo info = new ServiceInfo(serviceName, senderIP, servicePort);
                            activeServices.put(serviceName, info);
                            System.out.println("Registered: " + serviceName + " at " + senderIP + ":" + servicePort);
                        }
                    }

                    // Clear buffer for next packet
                    incomingData = new byte[1024];

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Heartbeat Checker Thread - removes dead services
    static class HeartbeatChecker extends Thread {
        private final long TIMEOUT_MS = 120000; // 120 seconds
        private final long CHECK_INTERVAL_MS = 30000; // Check every 30 seconds

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);

                    long currentTime = System.currentTimeMillis();

                    // Check each service
                    for (String serviceName : activeServices.keySet()) {
                        ServiceInfo info = activeServices.get(serviceName);
                        long timeSinceLastHeartbeat = currentTime - info.lastHeartbeat;

                        if (timeSinceLastHeartbeat > TIMEOUT_MS) {
                            // Remove dead service
                            activeServices.remove(serviceName);
                            System.out.println("REMOVED (timeout): " + serviceName +
                                    " (no heartbeat for " + timeSinceLastHeartbeat + "ms)");
                        }
                    }

                    // Print active services
                    if (!activeServices.isEmpty()) {
                        System.out.println("\n=== Active Services ===");
                        for (ServiceInfo info : activeServices.values()) {
                            System.out.println("  " + info);
                        }
                        System.out.println("=======================\n");
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Router Starting ===");

        // Start heartbeat listener thread
        HeartbeatListener listener = new HeartbeatListener();
        listener.start();

        // Start heartbeat checker thread
        HeartbeatChecker checker = new HeartbeatChecker();
        checker.start();

        System.out.println("Router is running...");
        System.out.println("Waiting for service nodes to register...\n");

        // Main thread keeps program alive
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}