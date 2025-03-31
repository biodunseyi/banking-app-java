package csci2020u.lab10.client;

import java.io.*;
import java.net.*;
import java.util.*;

public class ATMClient {
    private Socket socket;
    private PrintWriter networkOut;
    private BufferedReader networkIn;

    public static String SERVER_ADDRESS = "localhost";
    public static int SERVER_PORT = 16789;

    boolean auth = false;

    private final ATMGUI atmGUI;

    public ATMClient() {
        atmGUI = new ATMGUI();

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + SERVER_ADDRESS);
        } catch (IOException e) {
            System.err.println("IOException while connecting to server: " + SERVER_ADDRESS);
        }

        if (socket == null) {
            System.err.println("socket is null");
            System.exit(1);
        }

        try {
            networkOut = new PrintWriter(socket.getOutputStream(), true);
            networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("IOException while opening a read/write connection");
            System.exit(1);
        }

        try {
            atmGUI.setText(networkIn.readLine());
            if (getStatusCode(networkIn.readLine()) != 100) {
                System.out.println("Incorrect greeting from server, aborting");
                System.exit(1);
            }
        } catch (IOException e) {
            System.err.println("Error reading initial greeting from socket.");
            System.exit(1);
        }

        setUpButtons();
    }

    protected void processUserInput(int input) {
        try {
            switch (input) {
                case 0:
                    login();
                    break;
                case 1:
                    createNewAccount();
                    break;
                case 2:
                    logout();
                    break;
                case 3:
                    viewBalance();
                    break;
                case 4:
                    depositMoney();
                    break;
                case 5:
                    withdrawMoney();
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid command.");
        }
    }

    protected void login() {
        auth = false;
        setupBackButton();

        atmGUI.setText("Type your username:");

        ATMGUI.InputCallback passwordCallback = new ATMGUI.InputCallback() {
            @Override
            public void onInputRead(String password) {
                if (password.isEmpty() || password.contains(" ")) {
                    atmGUI.setText("Invalid password");
                    tryReadInput(this);
                } else {
                    networkOut.println("PWD " + password);
                    try {
                        String message = networkIn.readLine();
                        if (getStatusCode(message) != 200) {
                            setUpButtons("Login unsuccessful:<br>" + getStatusMessage(message));
                        } else {
                            auth = true;
                            setUpButtons();
                        }
                    } catch (IOException e) {
                        setUpButtons("Error reading response to PWD");
                    }
                }
            }
        };

        ATMGUI.InputCallback usernameCallback = new ATMGUI.InputCallback() {
            @Override
            public void onInputRead(String username) {
                if (username.isEmpty() || username.contains(" ")) {
                    atmGUI.setText("Invalid username");
                    tryReadInput(this);
                } else {
                    networkOut.println("UID " + username);
                    try {
                        String message = networkIn.readLine();
                        if (getStatusCode(message) != 100) {
                            setUpButtons("Something went wrong when trying to send the username.<br>Reason: " + getStatusMessage(message));
                            return;
                        }
                    } catch (IOException e) {
                        setUpButtons("Error reading response to UID.");
                        return;
                    }
                    atmGUI.setText("Type your passcode:");
                    tryReadInput(passwordCallback);
                }
            }
        };

        tryReadInput(usernameCallback);
    }

    protected void createNewAccount() {
        setupBackButton();

        atmGUI.setText("Choose a username for your new account:");

        ATMGUI.InputCallback usernameCallback = new ATMGUI.InputCallback() {
            @Override
            public void onInputRead(String username) {
                if (username.isEmpty() || username.contains(" ")) {
                    atmGUI.setText("Invalid username. Try again.");
                    tryReadInput(this);
                } else {
                    atmGUI.setText("Choose a password:");

                    ATMGUI.InputCallback passwordCallback = new ATMGUI.InputCallback() {
                        @Override
                        public void onInputRead(String password) {
                            if (password.isEmpty() || password.contains(" ")) {
                                atmGUI.setText("Invalid password. Try again.");
                                tryReadInput(this);
                            } else {
                                networkOut.println("NEW " + username + " " + password);

                                try {
                                    String response = networkIn.readLine();
                                    int statusCode = getStatusCode(response);

                                    if (statusCode == 201) {
                                        setUpButtons("Account created successfully!");
                                    } else {
                                        setUpButtons("Account creation failed:<br>" + getStatusMessage(response));
                                    }
                                } catch (IOException e) {
                                    setUpButtons("Error reading response from server:<br>" + e);
                                }
                            }
                        }
                    };

                    tryReadInput(passwordCallback);
                }
            }
        };

        tryReadInput(usernameCallback);
    }


    protected void logout() {
        networkOut.println("LOGOUT");

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    protected void viewBalance() {
        networkOut.println("VIEW");

        try {
            String message = networkIn.readLine();
            int statusCode = getStatusCode(message);

            if (statusCode == 200) {
                atmGUI.setText("Account balance:<br>$" + getStatusMessage(message));
            } else {
                atmGUI.setText("Error retrieving balance from the server.<br>Reason: " + getStatusMessage(message));
            }
        } catch (IOException e) {
            atmGUI.setText("Error reading information from the server:<br>" + e);
        }
    }

    protected void depositMoney() {
        networkOut.println("DEP");

        String message;

        try {
            message = networkIn.readLine();
            int statusCode = getStatusCode(message);

            if (statusCode != 100) {
                setUpButtons("Error retrieving balance from the server.<br>Reason: " + getStatusMessage(message));
                return;
            }
        } catch (IOException e) {
            setUpButtons("Error reading information from the server:<br>" + e);
            return;
        }

        setupBackButton("DEP");

        atmGUI.setText("Enter the amount you would like to deposit<br>Account balance:<br>$" + getStatusMessage(message));
        final Integer[] amount = {null};

        ATMGUI.InputCallback callback = new ATMGUI.InputCallback() {
            @Override
            public void onInputRead(String input) {
                try {
                    amount[0] = Integer.valueOf(input);
                } catch (NumberFormatException e) {
                    atmGUI.setText("Enter a valid number");
                }

                if (amount[0] == null) {
                    tryReadInput(this);
                    return;
                }

                networkOut.println("DEP " + amount[0]);

                try {
                    String message = networkIn.readLine();
                    int statusCode = getStatusCode(message);

                    if (statusCode == 202) {
                        setUpButtons("Account balance:<br>$" + getStatusMessage(message));
                    } else {
                        setUpButtons("Error retrieving balance from the server.<br>Reason: " + getStatusMessage(message));
                    }
                } catch (IOException e) {
                    setUpButtons("Error reading information from the server:<br>" + e);
                }
            }
        };
        tryReadInput(callback);
    }

    protected void withdrawMoney() {
        networkOut.println("WITH");

        String message;
        int balance;

        try {
            message = networkIn.readLine();
            int statusCode = getStatusCode(message);
            if (statusCode != 100) {
                setUpButtons("Error checking balance:<br>" + getStatusMessage(message));
                return;
            }
            balance = Integer.parseInt(getStatusMessage(message));
        } catch (Exception e) {
            setUpButtons("Error retrieving balance:<br>" + e.getMessage());
            return;
        }

        setupBackButton("WITH");

        atmGUI.setText("Enter amount to withdraw:<br>Account balance: $" + balance);
        final int finalBalance = balance;

        ATMGUI.InputCallback callback = new ATMGUI.InputCallback() {
            @Override
            public void onInputRead(String input) {
                int amount;
                try {
                    amount = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    atmGUI.setText("Please enter a valid number.");
                    tryReadInput(this);
                    return;
                }

                if (amount < 1 || amount > finalBalance) {
                    atmGUI.setText("Invalid amount. Must be between $1 and $" + finalBalance);
                    tryReadInput(this);
                    return;
                }

                networkOut.println("WITH " + amount);

                try {
                    String response = networkIn.readLine();
                    int statusCode = getStatusCode(response);
                    String statusMsg = getStatusMessage(response);

                    if (statusCode == 200) {
                        setUpButtons("Withdrawal successful!<br>New balance: $" + statusMsg);
                    } else {
                        setUpButtons("Withdrawal failed:<br>" + statusMsg);
                    }
                } catch (IOException e) {
                    setUpButtons("Error reading from server:<br>" + e.getMessage());
                }
            }
        };

        tryReadInput(callback);
    }

    protected int getStatusCode(String message) {
        StringTokenizer st = new StringTokenizer(message);
        String code = st.nextToken();
        return Integer.parseInt(code);
    }

    protected String getStatusMessage(String message) {
        StringTokenizer st = new StringTokenizer(message);
        String code = st.nextToken();
        if (st.hasMoreTokens()) {
            return message.substring(code.length() + 1);
        }
        return null;
    }

    private void setUpButtons() {
        setUpButtons(null);
    }

    private void setUpButtons(String message) {
        atmGUI.removeAllActionListeners();
        String[] labels = {
                "Login", "Create Account", "Quit", "View Balance", "Deposit Money", "Withdraw Money"
        };

        for (int i = 0; i < (auth ? 6 : 3); i++) {
            int index = i;
            atmGUI.addActionListener(i, _ -> {
                processUserInput(index);
            }, labels[i]);
        }
        atmGUI.disableInput();

        if (auth) {
            atmGUI.setText("Choose an option<br>" + (message != null ? "<br>" + message : ""));
        } else {
            atmGUI.setText("Welcome to the ATM machine<br>" + (message != null ? "<br>" + message : ""));
        }
    }

    protected void tryReadInput(ATMGUI.InputCallback callback) {
        atmGUI.getInput(callback);
    }

    protected void setupBackButton() {
        setupBackButton(null);
    }

    protected void setupBackButton(String command) {
        atmGUI.removeAllActionListeners();
        atmGUI.addActionListener(2, _ -> {
            if (command != null)
                networkOut.println(command + " BREAK");
            setUpButtons();
        }, "Back");
    }

    public static void main(String[] args) {
        new ATMClient();
    }
}
