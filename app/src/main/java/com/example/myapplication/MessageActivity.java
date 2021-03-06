package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.myapplication.Adapter.MessageAdapter;
import com.example.myapplication.Usermodel.Chat;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MessageActivity extends AppCompatActivity {
    TextView classname;
    ImageView imgview;
    int i = 0;
    FirebaseUser fuser;
    RecyclerView recyclerView;
    EditText textMessage;
    ImageButton imgbtn;
    ImageButton backbtn;
    Intent intent;
    TextView nickname;
    TextView participant;
    MessageAdapter messageAdapter;
    List<Chat> mChats;
    List<String> user_num = new ArrayList<>();

    String sender_id;
    String sender_nickname;
    String rcv_nick;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        classname = findViewById(R.id.textView3);
        imgbtn = findViewById(R.id.btn_send);
        backbtn = findViewById(R.id.backbtn);
        nickname = findViewById(R.id.nickname);
        textMessage = findViewById(R.id.text_send);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        participant = findViewById(R.id.participant_num);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);



        intent = getIntent();
        final String class_name,class_code;
        String arr[] = intent.getStringArrayExtra("class");
        class_name = arr[0];
        class_code = arr[1];
        classname.setText(class_name);


        fuser = FirebaseAuth.getInstance().getCurrentUser();


        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference doc_ref = db.collection("Chats").document(class_code);

        readMessages(db,doc_ref,class_code);
        //participant.setText(Integer.toString());

        recyclerView.setLayoutManager(linearLayoutManager);


        doc_ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {

                readMessages(db,doc_ref,class_code);
                //participant.setText(Integer.toString());

            }
        });//msg update listener
        backbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        sender_nickname = null;
        db.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    System.out.println("serach user database");
                    QuerySnapshot q_snap = task.getResult();
                    for(QueryDocumentSnapshot q_doc : q_snap){
                        if(q_doc.get("email").equals(fuser.getEmail()))
                            set_nickname(q_doc.get("nickname").toString());
                    }
                }
            }
        });
        imgbtn.setOnClickListener(new View.OnClickListener() {//msg send button
            @Override
            public void onClick(View v) {

                String msg = textMessage.getText().toString();
                Map<String,Object> upload_msg = new HashMap<>();
                Map<String,Object> upload = new HashMap<>();


                System.out.println("**********************");
                System.out.println(sender_id+" "+sender_nickname);
                upload_msg.put("id",fuser.getEmail());
                upload_msg.put("nickname",sender_nickname);
                upload_msg.put("msg",msg);
                Date time = Calendar.getInstance().getTime();
                upload_msg.put("time",time);
                String strtime = time.toString();
                System.out.println("**********************");
                System.out.println(upload_msg);
                System.out.println("**********************");
                String str = "msg "+strtime;
                upload.put(str,upload_msg);
                db.collection("Chats").document(class_code).set(upload,SetOptions.merge())
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                System.out.println("Data commit to firestore success");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("Database upload failure");
                    }
                });

                textMessage.setText("");
            }
        });
    }
    private void set_nickname(String str){
        sender_nickname = str;
    }
    public static LinkedHashMap<String, Object> sortMap(Map<String,Object> map){
        List<Map.Entry<String,Object>> entries = new LinkedList<>(map.entrySet());
        Collections.sort(entries,(o1,o2)->o1.getKey().compareTo(o2.getKey()));
        LinkedHashMap<String, Object> res = new LinkedHashMap<>();
        for(Map.Entry<String,Object> entry : entries)
            res.put(entry.getKey(),entry.getValue());
        return res;

    }//sort by timestamp

    private void readMessages(FirebaseFirestore db, DocumentReference doc_ref, final String class_code){
        mChats = new ArrayList<>();

        db.collection("Chats").document(class_code).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                user_num.clear();
                mChats.clear();
                String msg = new String();
                Timestamp rcv_time;
                String chattime = new String();
                if(task.isSuccessful()){
                    DocumentSnapshot doc_snap = task.getResult();
                    if(doc_snap.exists()) {
                        Map<String, Object> doc_data = doc_snap.getData();
                        doc_data = sortMap(doc_data);
                        Object[] arr = doc_data.values().toArray();

                        int i = 0;
                        for (Object obj : arr) {
                            Map<String, String> map = (HashMap<String, String>) obj;

                            //sorting by time
                            Iterator it = map.entrySet().iterator();
                            while (it.hasNext()) {
                                Map.Entry pair = (Map.Entry) it.next();
                                if (pair.getKey().equals("id")) {
                                    sender_id = pair.getValue().toString();
                                    if(user_num.contains(sender_id) == false){
                                        user_num.add(sender_id);
                                    }
                                }
                                if(pair.getKey().equals("nickname")){
                                    if(pair.getValue() == null)
                                        rcv_nick = "";
                                    else rcv_nick = pair.getValue().toString();
                                }
                                if (pair.getKey().equals("msg")) {
                                    msg = pair.getValue().toString();
                                    //System.out.println(fuser.getEmail());
//                                    if (sender.equals(fuser.getEmail()))
                                }
                                if(pair.getKey().equals("time")){
                                    rcv_time = (Timestamp)pair.getValue();
                                    Date dt = rcv_time.toDate();
                                    int hour,min;
                                    String day = "오전";
                                    String night = "오후";

                                    hour = dt.getHours();
                                    min = dt.getMinutes();
                                    if(hour > 13){
                                        hour -= 12;
                                        chattime = night+" "+hour+":"+min;
                                    }else{
                                        chattime = day+" "+hour+":"+min;
                                    }
                                }
                                it.remove();
                            }
                            participant.setText(Integer.toString(user_num.size()));
                            Chat chat = new Chat(sender_id,rcv_nick, class_code,msg,chattime,Integer.toString(user_num.size()));
                            mChats.add(chat);
                        }

                        messageAdapter = new MessageAdapter(MessageActivity.this, mChats, null);
                        recyclerView.setAdapter(messageAdapter);
                    }
                }
            }
        });


    }
}