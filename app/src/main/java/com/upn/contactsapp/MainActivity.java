package com.upn.contactsapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.upn.contactsapp.activities.CreateContactActivity;
import com.upn.contactsapp.activities.LoginActivity;
import com.upn.contactsapp.adapters.ContactAdaptar;
import com.upn.contactsapp.daos.ContactDAO;
import com.upn.contactsapp.entities.Contact;
import com.upn.contactsapp.services.ContactService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 10; // Cantidad de datos por página
    private int currentPage = 1; // Página actual
    private boolean isLoading = false; // Controla si se está cargando más datos
    private List<Contact> elementos = new ArrayList<>();
    private ContactAdaptar adaptar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = getSharedPreferences("com.upn.contactsapp", Context.MODE_PRIVATE);
        String token = sharedPref.getString("TOKEN", null);
        Log.i("LoginActivity", "TOKEN: " + token);

        if (token == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(this);
        ContactDAO contactDAO = db.contactDAO();

        // Cargar contactos iniciales desde la base de datos
        List<Contact> contacts = contactDAO.getAll();
        elementos.addAll(contacts);

        // Inicializar RecyclerView
        setUpRecyclerView();

        // Cargar los primeros datos
        loadContacts(currentPage);

        // Botón flotante para crear un nuevo contacto
        FloatingActionButton btnCreateContact = findViewById(R.id.btnCreateContact);
        btnCreateContact.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, CreateContactActivity.class);
            startActivityForResult(intent, 100);
        });
    }

    private void loadContacts(int page) {
        if (isLoading) return; // Evitar múltiples llamadas
        isLoading = true;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://66d5b903f5859a7042673752.mockapi.io")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ContactService service = retrofit.create(ContactService.class);
        service.getAll(page, PAGE_SIZE).enqueue(new Callback<List<Contact>>() {
            @Override
            public void onResponse(Call<List<Contact>> call, Response<List<Contact>> response) {
                isLoading = false;
                if (response.isSuccessful()) {
                    elementos.addAll(response.body());
                    adaptar.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Contact>> call, Throwable throwable) {
                isLoading = false;
                Log.e("MAIN_APP", throwable.getMessage());
            }
        });
    }

    private void setUpRecyclerView() {
        RecyclerView rvContacts = findViewById(R.id.rvContacts);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));

        adaptar = new ContactAdaptar(elementos);
        rvContacts.setAdapter(adaptar);

        rvContacts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && layoutManager.findLastCompletelyVisibleItemPosition() == elementos.size() - 1) {
                    // Si hemos llegado al final de la lista, cargar más datos
                    currentPage++;
                    loadContacts(currentPage);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == 100) {
            String contactJson = data.getStringExtra("CONTACT");
            Contact contact = new Gson().fromJson(contactJson, Contact.class);

            elementos.add(contact);
            adaptar.notifyDataSetChanged();
        }
    }
}
