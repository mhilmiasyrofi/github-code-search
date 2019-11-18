package com.project.githubsearch.model;

public class Token {
    int id;
    String token;
    boolean used;
    
    public Token(int id, String token) {
        this.id = id;
        this.token = token;
        this.used = false;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }
    
    /**
     * @return the used
     */
    public boolean getUsed() {
        return used;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @param used the isUsed to set
     */
    public void setUsed(boolean used) {
        this.used = used;
    }
}