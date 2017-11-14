package com.ginkage.gamepad.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.ginkage.gamepad.R;

public class GamepadActivity extends Activity {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getFragmentManager()
        .beginTransaction()
        .add(R.id.fragment_container, new GamepadFragment())
        .commit();
  }
}
