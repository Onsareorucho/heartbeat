package org.example;

import java.net.*;
import org.json.JSONObject;
import java.util.Random;

public class ServiceNode {
    public static void main(String[] args) throws Exception {
        // Parse args
        String routerIP = args[0];
        int routerPort = Integer.parseInt(args[1]);
        String serviceName = args[2];
        int servicePort = Integer.parseInt(args[3]);

        System.out.println("Starting " + serviceName + " on port " + servicePort);
        System.out.println("Sending heartbeats to " + routerIP + ":" + routerPort);

        // Create UDP socket
        DatagramSocket socket = new DatagramSocket();
        Random random = new Random();

        while (true) {
            // Build JSON heartbeat
            JSONObject json = new JSONObject();
            json.put("type", "HEARTBEAT");
            json.put("service", serviceName);
            json.put("port", servicePort);

            // Convert to bytes
            byte[] buffer = json.toString().getBytes();

            // Create and send packet
            InetAddress address = InetAddress.getByName(routerIP);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, routerPort);
            socket.send(packet);

            System.out.println("Heartbeat sent: " + json.toString());

            // Sleep random 15-30 seconds
            int sleepTime = 15000 + random.nextInt(15001);
            Thread.sleep(sleepTime);
        }
    }
}