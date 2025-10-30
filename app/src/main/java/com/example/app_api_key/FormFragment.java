package com.example.app_api_key;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FormFragment extends Fragment {

    public interface OnPlaceAddedListener {
        void onPlaceAdded(FamilyPlace place);
        void onClearAllRequested();
        void onRemoveLastRequested();
        void onCenterOnMyLocationRequested();
    }

    private OnPlaceAddedListener listener;
    private ExecutorService executorService;

    private EditText etName;
    private Spinner spRelation;
    private Spinner spColor;
    private EditText etAddress;
    private Button btnAdd;
    private Button btnClearAll;
    private Button btnRemoveLast;
    private Button btnMyLocation;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnPlaceAddedListener) {
            listener = (OnPlaceAddedListener) context;
        } else {
            throw new IllegalStateException("Activity must implement FormFragment.OnPlaceAddedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_form, container, false);
        etName = root.findViewById(R.id.etName);
        spRelation = root.findViewById(R.id.spRelation);
        spColor = root.findViewById(R.id.spColor);
        etAddress = root.findViewById(R.id.etAddress);
        btnAdd = root.findViewById(R.id.btnAdd);
        btnClearAll = root.findViewById(R.id.btnClearAll);
        btnRemoveLast = root.findViewById(R.id.btnRemoveLast);
        btnMyLocation = root.findViewById(R.id.btnMyLocation);

        List<String> relations = Arrays.asList("Padre", "Madre", "Abuelo", "Abuela", "Tío", "Tía", "Primo", "Prima", "Otro");
        ArrayAdapter<String> relationAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, relations);
        relationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRelation.setAdapter(relationAdapter);

        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(requireContext(), R.array.marker_colors, android.R.layout.simple_spinner_item);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spColor.setAdapter(colorAdapter);

        executorService = Executors.newSingleThreadExecutor();

        btnAdd.setOnClickListener(v -> onAddClicked());
        btnClearAll.setOnClickListener(v -> onClearAllClicked());
        btnRemoveLast.setOnClickListener(v -> onRemoveLastClicked());
        btnMyLocation.setOnClickListener(v -> { if (listener != null) listener.onCenterOnMyLocationRequested(); });
        return root;
    }

    private void onClearAllClicked() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar borrado")
                .setMessage("¿Seguro que deseas eliminar todas las direcciones y marcadores?")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                    if (listener != null) listener.onClearAllRequested();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void onRemoveLastClicked() {
        if (listener != null) listener.onRemoveLastRequested();
        {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar borrado")
                    .setMessage("¿Seguro que deseas eliminar la última dirección y el marcador?")
                    .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                        if (listener != null) listener.onClearAllRequested();
                    })
                    .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    private void onAddClicked() {
        final String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        final String relation = (String) spRelation.getSelectedItem();
        final String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), "El nombre es obligatorio", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(requireContext(), "La dirección es obligatoria", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Geocodificando...", Toast.LENGTH_SHORT).show();

        executorService.execute(() -> {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            try {
                List<Address> results = geocoder.getFromLocationName(address, 1);
                if (results == null || results.isEmpty()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(requireContext(), "No se encontró la dirección", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                Address addr = results.get(0);
                double lat = addr.getLatitude();
                double lng = addr.getLongitude();
                FamilyPlace place = new FamilyPlace(name, relation, address, lat, lng);
                Object sel = spColor.getSelectedItem();
                String selectedColor = sel != null ? sel.toString() : null;
                place.colorName = selectedColor;

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) {
                        listener.onPlaceAdded(place);
                        etAddress.setText("");
                        etName.setText("");
                        spRelation.setSelection(0);
                    }
                });
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(requireContext(), "Error de red/servicio de geocodificación", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(requireContext(), "Ocurrió un error al geocodificar", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}


