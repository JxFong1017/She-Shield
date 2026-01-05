package com.example.grpassignment;

public class TrustedContact {
    private String id;
    private String name;
    private String phone;
    private int rank;
    private String linkedUserId;
    // Empty constructor for Firestore
    public TrustedContact() {}

    public TrustedContact(String name, String phone, int rank, String linkedUserId) {
        this.name = name;
        this.phone = phone;
        this.rank = rank;
        this.linkedUserId = linkedUserId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getLinkedUserId() { return linkedUserId; }
    public void setLinkedUserId(String linkedUserId) { this.linkedUserId = linkedUserId; }
}