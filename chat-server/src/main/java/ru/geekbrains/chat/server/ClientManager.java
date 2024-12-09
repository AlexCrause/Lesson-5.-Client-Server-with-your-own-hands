package ru.geekbrains.chat.server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientManager implements Runnable {

    private final Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String name;

    public final static ArrayList<ClientManager> clients = new ArrayList<>();

    public ClientManager(Socket socket) {

        this.socket = socket;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            name = bufferedReader.readLine();
            clients.add(this);
            System.out.println(name + " подключился к чату.");
            broadcastMessage("Сервер: " + name + " подключился к чату.");

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        while (socket.isConnected()) {
            try {
                messageFromClient = bufferedReader.readLine();
                /*if (messageFromClient != null) {
                    //для macOS
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }*/
                if (messageFromClient.startsWith("@")) { // Проверка на личное сообщение
                    String[] messageParts = messageFromClient.split(":", 2);
                    String recipientName = messageParts[0].substring(1).trim(); // Имя получателя
                    String privateMessage = messageParts[1].trim(); // Текст сообщения
                    sendPrivateMessage(recipientName, privateMessage);
                } else {
                    // Рассылка общего сообщения всем, кроме отправителя
                    broadcastMessage(messageFromClient);
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }

    public void broadcastMessage(String message) {
        for (ClientManager client : clients) {
            // Убедимся, что сообщение не идет обратно к отправителю
            if (!client.name.equals(name)) {
                try {
                    client.bufferedWriter.write(message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    private void sendPrivateMessage(String recipientName, String message) {
        for (ClientManager client : clients) {
            // Отправляем сообщение только указанному получателю
            if (client.name.equals(recipientName)) {
                try {
                    client.bufferedWriter.write(name + ": " + message);
                    client.bufferedWriter.newLine();
                    client.bufferedWriter.flush();
                    return; // Прекращаем цикл, если нашли получателя
                } catch (IOException e) {
                    closeEverything(socket, bufferedReader, bufferedWriter);
                }
            }
        }
    }

    private void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {

        // Удаление клиента из коллекции
        removeClient();
        try {
            // Завершаем работу буфера на чтение данных
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            // Завершаем работу буфера для записи данных
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            // Закрытие соединения с клиентским сокетом
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeClient() {
        clients.remove(this);
        System.out.println(name + " покинул чат.");
        broadcastMessage("Сервер: " + name + " покинул чат.");
    }
}
