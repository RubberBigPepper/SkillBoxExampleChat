package com.albakm.skillboxexamplechat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.AndroidException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements Server.ServerListener, MessageController.ControllerListener {
    public static String myName = "";

    private RecyclerView chatWindow;
    private MessageController controller;
    private Server server;
    private TextView textViewUserCount;
    private Toast mToast;
    private EditText chatInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        server = new Server(this);//запускаем серверную часть
        server.connect();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_name);
        final EditText input = new EditText(this);
        input.setHint(R.string.enter_username_hint);
        SharedPreferences preferences=getSharedPreferences("options",MODE_PRIVATE);
        input.setText(preferences.getString("user name",""));
        builder.setView(input);
        builder.setPositiveButton(R.string.enter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                myName = input.getText().toString();
                server.sendName(myName);
                SharedPreferences.Editor editor=getSharedPreferences("options",MODE_PRIVATE).edit();
                editor.putString("user name",myName);//сохраним имя пользователя для дальнейшего использования
                editor.commit();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {//если пользователь отказался вводить имя-выходим
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();

        chatWindow = findViewById(R.id.chatWindow);

        controller = new MessageController(this);
//инициализация контроллера чата
        controller.setIncomingLayout(R.layout.incoming_message);
        controller.setOutgoingLayout(R.layout.outgoing_message);
        controller.setMessageTextId(R.id.messageText);
        controller.setMessageTimeId(R.id.messageTime);
        controller.setUserNameId(R.id.userName);
        controller.appendTo(chatWindow, this);

        textViewUserCount=findViewById(R.id.textViewUserCount);
        findViewById(R.id.btnViewAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewAllUsers();
            }
        });
//элементы ввода текста и отправки

        chatInput = findViewById(R.id.chatInput);
        Button sendMessage = findViewById(R.id.sendMessage);

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = chatInput.getText().toString();
                if (text.trim().length()==0)
                    return;//не будем отправлять пустой текст
                controller.addMessage(
                        new MessageController.Message(text, myName, true)
                );
                chatInput.setText("");
                server.sendMessage(text);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing())//выходим из программы, выключаем сервер
            server.disconnect();//остановим серверную часть
    }

    @Override
    public void onMessageReceived(final String name, final String text) {//пришло новое сообщения
        runOnUiThread(new Runnable() {//обработаем его в UI потоке
            @Override
            public void run() {
                controller.addMessage(//добавим в наш контроллер
                        new MessageController.Message(text, name, false)
                );
            }
        });
    }

    @Override
    public void onUserConnected(String name) {//покажем уведомление
        updateUserStatus(name, true);
    }

    @Override
    public void onUserDisconnected(String name) {
        updateUserStatus(name, false);
    }

    private String getAllUserCountString(){//форматированный вывод строки с количеством пользователей
        return String.format(Locale.US, "%s %d",
                getString(R.string.users_online), server.getUserCount());
    }

    private void updateUserStatus(final String name, final boolean bConnected){//обновляем заголовок приложения, укажем количество пользователей
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                makeToast(String.format(Locale.US,
                        getString(bConnected?R.string.user_connected:R.string.user_disconnected), name));
                textViewUserCount.setText(getAllUserCountString());
            }
        });
    }

    private void makeToast(final String message) {//чтобы при подключении тосты не маячили на каждого пользователя
        try{ //сделаем их реиспользуемыми
            if(mToast!=null&&mToast.getView().isShown())     // true if visible
                mToast.setText(message);
        } catch (Exception e) {         // invisible if exception
            mToast = null;//тост утрачен, будем заменять новым
        }
        if (mToast==null)
            mToast=Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();  //finally display it
    }

    private void viewAllUsers(){//просто покажем всех пользователей
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getAllUserCountString());
        final String[] userNames= server.getUserNames();
        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1,  userNames);
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {//клик по элементам списка нам не нужен пока
                String user= userNames[which];
                int pos=user.lastIndexOf('(');//отрежем лишнее
                if (pos>0)
                    user=user.substring(0,pos).trim();
                if(user.length()>0&&chatInput.getText().toString().length()==0) {
                    chatInput.setText(user + ", ");//если выбрано имя пользователя и пустое поле ввода - вставим имя пользователя
                    chatInput.setSelection(chatInput.getText().length());
                }
            }
        });
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {//и реакция на ОК тоже
                dialog.dismiss();//закроем диалог
            }
        });
        builder.show();
    }

    @Override
    public void onUserSelected(Long id) {
        String strUser=server.getUserName(id);
        if(strUser.length()>0&&chatInput.getText().toString().length()==0)
            chatInput.setText(strUser+", ");//если выбрано имя пользователя и пустое поле ввода - вставим имя пользователя
    }
}