package com.speedcam;

public class PresenterImpl implements Contract.Presenter {
    private Contract.MainView view;

    public PresenterImpl(Contract.MainView view) {
        this.view = view;
    }


    public Contract.MainView getView() {
        return view;
    }

    @Override
    public void onTakeView(Contract.MainView view) {
        if (view != null) {
            this.view = view;
        }
    }

    @Override
    public void onDropView() {
        this.view = null;
    }
}
