package org.example;

import java.net.*;
import org.json.JSONObject;
import java.util.Random;

public class ServiceNode {
    public static void main(String[] args) throws Exception {
        // Validate arguments
        if (args.length != 4) {
            System.out.println("Usage: java ServiceNode <routerIP> <routerUDPPort> <serviceName> <myTCPPort>");
            System.out.println("Example: java ServiceNode 3.87.45.123 9000 BASE64_ENCODE 8080");
            System.exit(1);
        }

        // Parse command-line arguments
        String routerIP = args[0];
        int routerUDPPort = Integer.parseInt(args[1]);
        String serviceName = args[2];
        int myServicePort = Integer.parseInt(args[3]);

        System.out.println("=== Service Node Starting ===");
        System.out.println("Service: " + serviceName);
        System.out.println("My TCP Port: " + myServicePort);
        System.out.println("Router: " + routerIP + ":" + routerUDPPort);
        System.out.println("Sending heartbeats every 15 seconds...");
        System.out.println("============================");

        // Create UDP socket
        DatagramSocket socket = new DatagramSocket();
        Random random = new Random();

        while (true) {
            // Build JSON heartbeat
            JSONObject json = new JSONObject();
            json.put("type", "HEARTBEAT");
            json.put("service", serviceName);
            json.put("port", myServicePort);

            // Convert to bytes
            byte[] buffer = json.toString().getBytes();

            // Create and send packet using command-line args
            InetAddress address = InetAddress.getByName(routerIP);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, routerUDPPort);
            socket.send(packet);

            System.out.println("[" + System.currentTimeMillis() + "] Heartbeat sent: " + json.toString());

            // Sleep 15 seconds
            int sleepTime = 15000 ;
            Thread.sleep(sleepTime);
        }
    }
}