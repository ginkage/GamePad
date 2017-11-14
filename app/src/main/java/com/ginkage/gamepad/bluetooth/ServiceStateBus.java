package com.ginkage.gamepad.bluetooth;

/** Helper interface to register for the profile proxy state changes. */
public interface ServiceStateBus {
  /**
   * Initiate the profile proxy service connection.
   *
   * @param listener Callback to listen for the service connection changes.
   */
  void register(ServiceStateListener listener);

  /** Close the connection with the profile proxy. */
  void unregister();
}
