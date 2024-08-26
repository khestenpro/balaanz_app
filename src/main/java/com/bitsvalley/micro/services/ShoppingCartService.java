package com.bitsvalley.micro.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ShoppingCartService {
  String getShoppingCartDetails(Long generalLedgerId, HttpServletRequest request) throws IOException;
  void generatePosReceiptPdf(long id, String htmlBuffer, HttpServletResponse response) throws IOException;
}
