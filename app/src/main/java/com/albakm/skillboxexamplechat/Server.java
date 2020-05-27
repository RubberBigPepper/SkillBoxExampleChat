package com.albakm.skillboxexamplechat;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final String TAG="SERVER";//тэг для логов

    interface ServerListener{//заведем интерфейс листенера, MainActivity по нему будет получать уведомления
        void onMessageReceived(String name, String text);
        void onUserConnected(String name);
        void onUserDisconnected(String name);
    };

    private ServerListener listener;
    private WebSocketClient client;
    private Map<Long, String> names = new ConcurrentHashMap<>();
    //private Consumer<Pair<String, String>> onMessageReceived;

    public Server(@NonNull ServerListener listener) {
        //this.onMessageReceived = onMessageReceived;
        this.listener=listener;
    }

    public void connect() {
        URI addr = null;
        try {
            addr = new URI("ws://35.214.3.133:8881");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        client = new WebSocketClient(addr) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i(TAG, "Connected to server");
                //подключение к серверу
               /* String connectionString=Protocol.packName(new Protocol.UserName(MainActivity.myName));
                Log.i(TAG,"Sending my name to server: "+connectionString);
                client.send(connectionString);*/
            }

            @Override
            public void onMessage(String jsonMessage) {
                //при получении нового сообщения, проверим его тип и обработаем в соответствии с типом
                switch(Protocol.getType(jsonMessage)) {
                    case Protocol.MESSAGE:
                        displayIncoming(Protocol.unpackMessage(jsonMessage));
                        break;
                    case Protocol.USER_STATUS:
                        userStatusChanged(Protocol.unpackStatus(jsonMessage));
                        break;
                    default:
                        break;
                }
                Log.i(TAG, "Got message from server: " + jsonMessage);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i(TAG, "Connection closed");
                //при отключении от сервера
            }

            @Override
            public void onError(Exception ex) {
                //при ошибке работы сервера
                Log.e(TAG, "Error occurred: " + ex.getMessage());
            }
        };

        client.connect();
    }

    public void disconnect() {
        client.close();
    }

    public void sendMessage(String text) {
        Protocol.Message mess = new Protocol.Message(text);
        if (client != null && client.isOpen()) {
            client.send(Protocol.packMessage(mess));
        }
    }

    public void sendName(String name) {
        Protocol.UserName userName = new Protocol.UserName(name);
        if (client != null && client.isOpen()) {
            client.send(Protocol.packName(userName));
        }
    }

    private void userStatusChanged(Protocol.UserStatus status) {//обработка отключения/подключения пользователей
        Protocol.User user = status.getUser();
        if (status.isConnected()) {
            names.put(user.getId(), user.getName());
            listener.onUserConnected(user.getName());
        } else {
            names.remove(user.getId());
            listener.onUserDisconnected(user.getName());
        }
    }

    private void displayIncoming(Protocol.Message message) {
        String name = names.get(message.getSender());
        if (name == null||name.length()==0) {//неизвестный пользователь
            name = "Unnamed";
        }
        listener.onMessageReceived(name,message.getEncodedText());
    }

    public int getUserCount(){//количество пользователей в чатике
        return names.size();
    }

    public String[] getUserNames(){//все имена пользователей, которые есть в чатике
        String[] res=new String[names.size()];
        Iterator<Long> usersIterator=names.keySet().iterator();
        int index=0;
        while (usersIterator.hasNext()) {
            long key = usersIterator.next();
            res[index++] = names.get(key)+" (id="+String.valueOf(key)+")";
        }
        return res;
    }

    public String getUserName(long userID){
        try{//попробуем найти имя пользователя
            return names.get(userID);
        }
        catch (Exception ex){
            return "";//иначе пустая строка
        }
    }
}