package com.example.chatapp.activities;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.adapters.UserAdapter;
import com.example.chatapp.databinding.ActivityUsersBinding;
import com.example.chatapp.listeners.UserListener;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class UsersActivity extends BaseActivity implements UserListener {

    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    private String textToSpeech,buttonSpeech;
    private ArrayList<String> nameUsers;
    private final int REQ_CODE = 100;
    private String test;
    TextToSpeech t1;
    UserAdapter userAdapter;
    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        mp = MediaPlayer.create(this, R.raw.demousers);
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });
        getUsers();
        setListeners();
        setContentView(binding.getRoot());
    }
    @SuppressLint("ClickableViewAccessibility")
    private void setListeners() {
        binding.imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonSpeech("Back Button Pressed");
                onBackPressed();
            }
        });
        binding.btnGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mp.isPlaying()){
                    mp.pause();
                }else{
                    mp.start();
                }
            }
        });
        binding.imageSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonSpeech("Speak out the full name");
                binding.imageSearch.setVisibility(View.GONE);
                binding.userSearch.setVisibility(View.VISIBLE);
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Need to speak");
                try {
                    startActivityForResult(intent, REQ_CODE);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(),"Sorry your device not supported",Toast.LENGTH_SHORT).show();
                }
                searchUser();
            }
        });
        binding.imageRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonSpeech("Reading out all users");
                textToSpeech = nameUsers.toString();
                readUser(textToSpeech);
            }
        });
        binding.usersRecycleView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(mp.isPlaying()){
                    mp.pause();
                    return true;
                }else{
                    mp.start();
                }
                return false;
            }
        });
    }
    private void getUsers(){
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                            if(task.isSuccessful() && task.getResult() != null){
                                //nameUsers.clear();
                                List<User> users = new ArrayList<>();
                                nameUsers = new ArrayList<>();
                                for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                                    if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                        continue;
                                    }

                                    User user = new User();
                                    user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                    user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                    user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                    user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                    user.id = queryDocumentSnapshot.getId();
                                    users.add(user);
                                    nameUsers.add(user.name);
                                }
                                if(users.size()>0){
                                    userAdapter = new UserAdapter(users,this);
                                    binding.usersRecycleView.setAdapter(userAdapter);
                                    binding.usersRecycleView.setVisibility(View.VISIBLE);
                                }
                                else{
                                    showErrorMessage();
                                }
                            }
                            else {
                                showErrorMessage();
                            }
                });
    }
    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s","No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }
    private void loading(Boolean isLoading){

        if (isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
        finish();
    }
    public void readUser(String textToSpeech){
        t1.speak(textToSpeech, TextToSpeech.QUEUE_FLUSH, null);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE: {
                if (resultCode == RESULT_OK && null != data) {
                    ArrayList result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    test = result.toString().substring(1, result.toString().length() - 1);
                    binding.userSearch.setText(test);
                }
                break;
            }
        }
    }
    private void searchUser(){
        binding.usersRecycleView.setVisibility(View.GONE);
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<User> users = new ArrayList<>();
                        nameUsers = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(Objects.equals(test, queryDocumentSnapshot.getString(Constants.KEY_NAME))){
                                User user = new User();
                                user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                user.id = queryDocumentSnapshot.getId();
                                users.add(user);
                                nameUsers.add(user.name);
                            }
                        }
                        if(users.size()>0){
                            userAdapter = new UserAdapter(users,this);
                            binding.usersRecycleView.setVisibility(View.VISIBLE);
                            binding.usersRecycleView.setAdapter(userAdapter);
                        }
                    }
                });
        binding.userSearch.setVisibility(View.GONE);
        binding.imageSearch.setVisibility(View.VISIBLE);
    }
    private void setButtonSpeech(String buttonSpeech){
        t1.speak(buttonSpeech, TextToSpeech.QUEUE_FLUSH, null);
    }

}