package ir.myket.developerapi;

import android.os.Build;


import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;

import ir.myket.developerapi.data.ApiResult;
import ir.myket.developerapi.data.Update;

public class MyketIABPlugin extends MyketIABPluginBase {
    private static MyketIABPlugin mInstance;
    private Core core;

    public MyketIABPlugin() {

    }

    public static MyketIABPlugin instance() {
        if (MyketIABPlugin.mInstance == null) {
            MyketIABPlugin.mInstance = new MyketIABPlugin();
        }
        return MyketIABPlugin.mInstance;
    }

    public void initialConnect() {
        core = new Core(getActivity());
        core.connect(new Core.CoreListener<Boolean>() {
            @Override
            public void onResult(Boolean response) {
                if (response) {
                    UnitySendMessage("connectedServiceSucceeded", "Service is connect :)");

                } else {
                    UnitySendMessage("connectedServiceFailed", "Service is not connect !");

                }
            }

            @Override
            public void onError(@NonNull ApiResult error) {
                if (error.getResponse() == Core.RESPONSE_RESULT_MYKET_UPDATE) {
                    UnitySendMessage("myketUpdateAvailable", "Please update myket !");

                } else if (error.getResponse() == Core.RESPONSE_RESULT_NEED_MYKET) {
                    UnitySendMessage("needInstallMyket", "Please install myket !");

                } else {
                    UnitySendMessage("connectedServiceFailed", error.getMessage());

                }
            }
        });
    }

    public void checkUpdate() {

        core.getAppUpdate(new Core.CoreListener<Update>() {
            @Override
            public void onResult(Update response) {
                if (!response.isUpdateAvailable()) {
                    UnitySendMessage("updateIsAvailable", "An update with versionCode= " + response.getVersionCode() + " Available!\n" +
                            "Whats New: " + response.getDescription());

                } else {
                    UnitySendMessage("appAlreadyUpdated", "App already updated :)");
                }
            }

            @Override
            public void onError(@NonNull ApiResult error) {
                UnitySendMessage("errorResponse", error.getMessage());
            }
        });
    }

    public void checkLogin() {
        core.isUserLogin(aBoolean -> {
            if (aBoolean) {
                UnitySendMessage("userIsLogin", "User is login :)");

            } else {
                UnitySendMessage("userIsNotLogin", "User is not login !");
            }
            return null;
        });

    }

    public void getUserId() {
        core.getUserId(getActivity(), new Core.CoreListener<String>() {
            @Override
            public void onResult(String response) {
                UnitySendMessage("userId", response);
            }

            @Override
            public void onError(@NonNull ApiResult error) {
                UnitySendMessage("errorResponse", error.getMessage());
            }
        });
    }

    public void saveData(String value) {
        byte[] data = (value).getBytes();
        core.saveData(getActivity(), data, new Core.CoreListener<Boolean>() {
            @Override
            public void onResult(Boolean response) {
                UnitySendMessage("dataSaved", "Data Saved :)");
            }

            @Override
            public void onError(@NonNull ApiResult error) {
                UnitySendMessage("dataNotSaved", error.getMessage());

            }
        });
    }

    public void loadData() {
        core.loadData(getActivity(), new Core.CoreListener<byte[]>() {
            @Override
            public void onResult(byte[] response) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    UnitySendMessage("dataLoaded", new String(response, StandardCharsets.UTF_8));
                } else {
                    UnitySendMessage("dataLoaded", new String(response));
                }
            }

            @Override
            public void onError(@NonNull ApiResult error) {
                UnitySendMessage("dataNotLoaded", error.getMessage());

            }
        });
    }


}