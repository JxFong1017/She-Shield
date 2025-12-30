package com.example.grpassignment;

public class TrustedContact {
    private String id;
    private String name;
    private String phone;
    private int rank;

    // Empty constructor for Firestore
    public TrustedContact() {}

    public TrustedContact(String name, String phone, int rank) {
        this.name = name;
        this.phone = phone;
        this.rank = rank;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getName() { return name; }
    public String getPhone() { return phone; }
}