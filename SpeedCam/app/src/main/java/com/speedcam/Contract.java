package com.speedcam;

public interface Contract {
    interface MainView {
        void showSpeed(Float speed);
        void showSignImage(int predictedLabel);
    }

    interface Presenter {
        void onTakeView(Contract.MainView view);
        void onDropView();
    }

}
