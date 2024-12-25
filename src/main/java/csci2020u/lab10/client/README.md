# Implementing Functionality of the Client

Here, you'll implement:

- `withdrawMoney()`
- `createNewAccount()`

There are also comments in the `ATMClient.java` file as well.
This lab also revolves around sending [HTTP status codes](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status) (although not using HTTP but websockets).

## Functions

Some useful functions that you should use to implement the functions are:

- `getStatusCode(String)`: Get status code from the response
- `getStatusMessage(String)`: Get message from the response
- `Integer.valueOf()`: Get the integer.
- `tryReadInput(InputCallback)`: Reveal an input field. When entered, call `InputCallback`.
- `setUpButtons() (String)`: Display the initial menu and, optionally, a message.
- `setupBackButton(String)`: Display the back button and break the server function.
- `atmGUI.setText(String)`: Display message.

Some exceptions you'll need to handle:

- `NumberFormatException`
- `IOException`

### `createNewAccount()`

In this function, the program should prompt the user for a username and password. The function should reject if either
the username or password is blank, a single space or null.

If the input is valid, then send a request of the following format to the server:

```sh
NEW <username> <password>
```

Possible responses from the server:

- on success, the server will return `201 Created`
    - you should display a small message saying that the account was successfully created
- on failure, the server will return `400 Username or password is invalid`
    - you should display a small message saying there was an error and the reason why
- on the server side, if there is an error during this process, the server will return `500 Internal server error`
    - you should display a small message saying there was an error and the reason why
- if there was an error reading a message from the server
    - you should display a small message saying there was an error and a stack trace

You'll need to handle all of above cases.

### `withdrawMoney()`

This function should have the following flow:

1. send an initial request to the server of form `WITH`
2. the server should then respond with `100 <the balance>`
    1. if the status code of this response isn't `100`
        1. display a small error message to `STDOUT` with the reason given
    2. if the status code of this response **is** `100`
        1. display a small message displaying `<the balance>`
3. if the balance itself is `0`
    1. display a small message displaying that the user cannot withdraw any money
4. after this, you'll then prompt the user for how much money they would like to withdraw
    1. if the user presses the `'Back'` button, send `WITH BREAK` to the server and break from the function
5. if the input is a number that is less or equal to the balance given by the server
    1. send a request of form `WITH <amount>` to the server

Possible responses from the server:

- on success, the server will return `200 <new balance>`
    - you should display a small message displaying the new balance sent by the server
- on failure, the server will return `400 Bad request`
    - you should display a small message saying there was an error and the reason why
- on the server side, if there is an error during this process, the server will return `500 Internal server error`
    - you should display a small message saying there was an error and the reason why
- if there was an error reading a message from the server
    - you should display a small message saying there was an error and a stack trace

You'll need to handle all of above cases.

>Look at the other functions implemented for you to guide your solution