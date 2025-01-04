# Lab 10 - ATM Client

CSCI 2020U: Software Systems Development and Integration

## Overview

In this lab, you'll need to implement the functionality of these functions:

In `ATMThread.java`:

- `processWITH(String)` this function withdraws money from a user's account
    - In reality, this deducts the amount passed by the user from the `balances` hashmap

In `ATMClient.java`:

- `withdrawMoney()` this function reads user input and sends a request to the server to withdraw $x$ amount of money
- `createNewAccount()` this function creates a new account on the server

## Tasks

See each subdirectory for instructions on what to do.

```dir
src/main/java/org/example
   client/
      README.md
      ATMClient.java
      ATMGUI.java
   server/
      README.md
      ATMServer.java
      ATMThread.jva
```
>The auto grader will not check for correctness, this will be manually done.

## How to Submit

### In session

(Preferably)

- Show your local and remote repositories to the TA to prove that you have finished this lab.

### After lab hours

(1 week to submit - before your next lab session)

- Link to your GitHub repository on Canvas
- Screenshots of the command line terminal
- Screenshots of the UI
- Add screenshots to `README.md`

The TA can provide oral feedback if you do not receive full marks for any lab assignment, but it is most
appropriate to ask the TA for this feedback in a timely fashion (i.e. ask now, not at the end of the term).
