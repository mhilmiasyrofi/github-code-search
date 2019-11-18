package com.project.githubsearch.model;

public class SynchronizedFeeder {
    Token token_1;
    Token token_2;
    Token token_3;
    
    public SynchronizedFeeder() {
        token_1 = new Token(1, System.getenv("GITHUB_AUTH_TOKEN_1"));
        token_2 = new Token(2, System.getenv("GITHUB_AUTH_TOKEN_2"));
        token_3 = new Token(3, System.getenv("GITHUB_AUTH_TOKEN_3"));
    }

    public synchronized Token getAvailableToken() {
        if (!token_1.getUsed()) {
            lockToken(1);
            return token_1;
        }
        if (!token_2.getUsed()) {
            lockToken(2);
            return token_2;
        }
        if (!token_3.getUsed()) {
            lockToken(3);
            return token_3;
        }
        return token_1;
    }

    public synchronized void lockToken(int id) {
        if (id == 1){
            token_1.setUsed(true);
        } else if (id == 2) {
            token_2.setUsed(true);
        } else if (id == 3) {
            token_3.setUsed(true);
        }
    }

    public synchronized void releaseToken(int id) {
        if (id == 1){
            token_1.setUsed(false);
        } else if (id == 2) {
            token_2.setUsed(false);
        } else if (id == 3) {
            token_3.setUsed(false);
        }
    }


    
    
}