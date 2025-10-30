package com.example.app_api_key;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

import java.util.UUID;

public class FamilyPlace implements Parcelable {
    public final String id;
    public final String name;
    public final String relation;
    public final String address;
    public final double lat;
    public final double lng;
    @androidx.annotation.Nullable
    public String colorName;

    public FamilyPlace(String name, String relation, String address, double lat, double lng) {
        this(UUID.randomUUID().toString(), name, relation, address, lat, lng, null);
    }

    public FamilyPlace(String id, String name, String relation, String address, double lat, double lng) {
        this(id, name, relation, address, lat, lng, null);
    }

    public FamilyPlace(String id, String name, String relation, String address, double lat, double lng, @androidx.annotation.Nullable String colorName) {
        this.id = id;
        this.name = name;
        this.relation = relation;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.colorName = colorName;
    }

    protected FamilyPlace(Parcel in) {
        id = in.readString();
        name = in.readString();
        relation = in.readString();
        address = in.readString();
        lat = in.readDouble();
        lng = in.readDouble();
        colorName = in.readString();
    }

    public static final Creator<FamilyPlace> CREATOR = new Creator<FamilyPlace>() {
        @Override
        public FamilyPlace createFromParcel(Parcel in) {
            return new FamilyPlace(in);
        }

        @Override
        public FamilyPlace[] newArray(int size) {
            return new FamilyPlace[size];
        }
    };

    public LatLng toLatLng() {
        return new LatLng(lat, lng);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(relation);
        dest.writeString(address);
        dest.writeDouble(lat);
        dest.writeDouble(lng);
        dest.writeString(colorName);
    }
}


