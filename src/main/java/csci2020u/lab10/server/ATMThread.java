package csci2020u.lab10.server;

import java.net.Socket;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ATMThread extends Thread {
    protected String id;
    protected Socket socket;
    protected PrintWriter out = null;
    protected BufferedReader in = null;

    protected String attempted_user = null;
    protected String attempted_pass = null;

    protected boolean auth = false;
    protected String user = null;

    protected HashMap<String, byte[]> users;
    protected HashMap<String, Integer> balances;

    protected final static String PWD = "PWD";
    protected final static String UID = "UID";
    protected final static String NEW = "NEW";
    protected final static String DEP = "DEP";
    protected final static String WITH = "WITH";
    protected final static String VIEW = "VIEW";
    protected final static String LOGOUT = "LOGOUT";

    protected final static String[] COMMANDS = {
            PWD, UID, NEW, DEP, WITH, VIEW, LOGOUT
    };

    public ATMThread(String _id, Socket _socket, HashMap<String, byte[]> _users,
                     HashMap<String, Integer> _balances) {
        super();
        this.id = _id;
        this.socket = _socket;
        this.users = _users;
        this.balances = _balances;

        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            this.err("IOException while opening a read/write connection");
        }
    }

    public void run() {
        out.println("Welcome to the ATM Machine");
        out.println("100 Ready");

        while (processCommand());

        if (user != null) {
            this.log(user + " disconnected");
        } else {
            this.log("Client disconnected");
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean processCommand() {
        String message;

        try {
            message = in.readLine();
        } catch (IOException e) {
            this.err("Error reading command from socket.");
            return false;
        }

        if (message == null) {
            return true;
        }

        StringTokenizer st = new StringTokenizer(message);
        String command = st.nextToken();
        String args = null;
        if (st.hasMoreTokens()) {
            args = message.substring(command.length() + 1);
        }

        return processCommand(command, args);
    }

    public boolean processCommand(String command, String arguments) {
        command = command.toUpperCase();

        this.log("Caught command " + command + " from the user");

        if (!isValidCommand(command)) {
            out.println("404 Unrecognized Command: " + command);
            return true;
        }

        switch (command.toUpperCase()) {
            case LOGOUT:
                return logout();
            case UID:
                return processUID(arguments);
            case PWD:
                return processPWD(arguments);
            case NEW:
                return processNEW(arguments);
            default:
                break;
        }

        if (auth) {
            this.log("Allowing authorized commands for user: " + user);
            switch (command.toUpperCase()) {
                case VIEW:
                    return processVIEW();
                case WITH:
                    return processWITH(arguments);
                case DEP:
                    return processDEP(arguments);
                default:
                    break;
            }
        } else {
            out.println("401 Unauthenticated user");
            this.log("User tried to send a command without authorization");
        }

        return true;
    }

    protected boolean processUID(String argument) {
        attempted_user = argument;
        out.println("100 Continue");
        return true;
    }

    protected boolean processPWD(String password) {
        if (attempted_user == null) {
            out.println("400 No username set when sending password");
            return true;
        }

        byte[] hashed = users.get(attempted_user);
        if (hashed == null) {
            out.println("404 No such user exists");
            return true;
        }

        byte[] attempted_hash;
        try {
            attempted_hash = MessageDigest.getInstance("MD5").digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            this.err("Could not find algorithm:\n" + e);
            out.println("500 Internal server error");
            return false;
        }

        if (isSameHash(hashed, attempted_hash)) {
            auth = true;
            user = attempted_user;
            this.log(user + " logged into the server");
            out.println("200 Login successful");
        } else {
            out.println("404 Incorrect login");
        }

        attempted_user = null;
        attempted_pass = null;

        return true;
    }

    protected boolean processNEW(String argument) {
        if (argument == null) {
            this.log("Client gave an empty request.");
            out.println("400 Username or password is invalid");
            return true;
        }

        String username, password;
        try {
            String[] parts = argument.split(" ");
            username = parts[0];
            password = parts[1];
        } catch (Exception e) {
            this.log("Client gave an incorrect request");
            out.println("400 Username or password is invalid");
            return true;
        }

        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            this.err("Could not find algorithm:\n" + e);
            out.println("500 Internal server error");
            return false;
        }

        this.log("Creating a new account for " + username + " with a balance of zero.");
        users.put(username, hash);
        balances.put(username, 0);
        out.println("201 Created");

        return true;
    }

    protected boolean processDEP(String argument) {
        if (argument == null) {
            if (!processVIEW("100"))
                return false;
            return true;
        }

        if (argument.equalsIgnoreCase("BREAK")) {
            this.log(DEP + " Nothing further from the user.");
            return true;
        }

        Integer amount;
        try {
            amount = Integer.valueOf(argument);
        } catch (NumberFormatException e) {
            this.err(DEP + " User " + user + " didn't give a number.");
            out.println("400 Bad request");
            return false;
        }

        balances.put(user, balances.get(user) + amount);
        this.log("Incremented balance of " + user + " by " + amount);
        out.println("202 " + balances.get(user));

        return true;
    }

    protected boolean processWITH(String argument) {
        if (argument == null) {
            return processVIEW("100");
        }

        if (argument.equalsIgnoreCase("BREAK")) {
            this.log("WITH Nothing further from the user.");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            this.err("WITH User " + user + " didn't provide a valid number.");
            out.println("400 Bad request");
            return true;
        }

        Integer currentBalance = balances.get(user);
        if (currentBalance == null) {
            this.err("User " + user + " has no balance.");
            out.println("500 Internal server error");
            return true;
        }

        if (amount > currentBalance) {
            out.println("400 Bad request");
            return true;
        }

        balances.put(user, currentBalance - amount);
        this.log("Decremented balance of " + user + " by " + amount);
        out.println("200 " + balances.get(user));

        return true;
    }

    protected boolean processVIEW() {
        return processVIEW("200");
    }

    protected boolean processVIEW(String code) {
        Integer balance = balances.get(user);

        if (balance == null) {
            this.err("User " + user + " has no balance.");
            out.println("500 Internal server error");
            return false;
        }

        out.println(code + " " + balance);
        return true;
    }

    protected boolean logout() {
        return false;
    }

    protected void log(String message) {
        System.out.println(this.id + ": " + message);
    }

    protected void err(String message) {
        System.err.println(this.id + ": " + message);
    }

    protected boolean isValidCommand(String target) {
        for (String e : ATMThread.COMMANDS) {
            if (e.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSameHash(byte[] a, byte[] b) {
        if (a.length != b.length)
            return false;
        for (int i = 0; i < a.length; ++i) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }
}