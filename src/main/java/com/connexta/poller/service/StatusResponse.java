/*
 * Copyright (c) 2019 Connexta, LLC
 *
 * Released under the GNU Lesser General Public License version 3; see
 * https://www.gnu.org/licenses/lgpl-3.0.html
 */
package com.connexta.poller.service;

public interface StatusResponse {

  // TODO: Lombok and Jackson are not playing well together. Had to remove all Lombok annotations.
  // Not sure what I was doing wrong. Solved by creating getter/setter methods.
  String getStatus();

  void setStatus(String status);
}
