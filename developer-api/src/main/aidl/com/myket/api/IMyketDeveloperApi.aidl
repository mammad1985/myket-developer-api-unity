package com.myket.api;

// Declare any non-default types here with import statements

interface IMyketDeveloperApi {
    int isDeveloperApiSupported(int apiVersion);

    Bundle getAppUpdateState(int apiVersion, String packageName);

    int isUserLogin(int apiVersion);

    Bundle getAccountInfo(int apiVersion, String packageName);

    Bundle saveData(int apiVersion, String packageName, String payload);

    Bundle loadData(int apiVersion, String packageName);
}