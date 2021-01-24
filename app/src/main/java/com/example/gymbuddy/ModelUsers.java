package com.example.gymbuddy;
//author : Osman Batuhan Şahin

public class ModelUsers {

    //use same name in firebase
    String name, email, search, profilepic, nickname, bio, uid;

    public ModelUsers(){
    }

    public ModelUsers(String name, String email, String search, String profilepic, String nickname, String bio, String uid) {
        this.name = name;
        this.email = email;
        this.search = search;
        this.profilepic = profilepic;
        this.nickname = nickname;
        this.bio = bio;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUseremail() {
        return email;
    }

    public void setUseremail(String email) {
        this.email = email;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public String getImageLink() {
        return profilepic;
    }

    public String getProfilepic(){
        return profilepic;
    }

    public void setImageLink(String profilepic) {
        this.profilepic = profilepic;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
