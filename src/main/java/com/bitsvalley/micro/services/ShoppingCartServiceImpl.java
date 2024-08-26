package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.GeneralLedger;
import com.bitsvalley.micro.domain.OrderItem;
import com.bitsvalley.micro.domain.ShoppingCart;
import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.model.CartDetail;
import com.bitsvalley.micro.model.CartUserDetails;
import com.bitsvalley.micro.model.PosReceiptPdf;
import com.bitsvalley.micro.repositories.GeneralLedgerRepository;
import com.bitsvalley.micro.repositories.OrderItemRepository;
import com.bitsvalley.micro.repositories.ShoppingCartRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import com.itextpdf.html2pdf.HtmlConverter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

  public static final String SHOPPING_CART_NOT_EXIST = "SHOPPING_CART_NOT_EXIST";
  public static final String N_A = "N/A";

  private final ShoppingCartRepository shoppingCartRepository;
  private final OrderItemRepository orderItemRepository;
  private final GeneralLedgerRepository generalLedgerRepository;
  private final UserRepository userRepository;
  private final PdfService pdfService;

  @Override
  public String getShoppingCartDetails(Long generalLedgerId, HttpServletRequest request) throws IOException {
    RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
    String logoPath = (runtimeSetting.getUnionLogo());
    String slogan = runtimeSetting.getSlogan();
    String businessName = runtimeSetting.getBusinessName();
    Optional<GeneralLedger> byId = generalLedgerRepository.findById(generalLedgerId);
    GeneralLedger generalLedger = byId.orElseThrow(() -> new RuntimeException("NO_TRANSACTION_EXIST"));
    String reference = generalLedger.getReference();
    List<OrderItem> orderItems = fetchShoppingCartItems(Long.valueOf(generalLedger.getAccountNumber()));
    OrderItem cartItemsExist = orderItems.stream().findAny().orElseThrow(() -> new RuntimeException("NO_CART_ITEMS_EXIST"));
    User user = null != cartItemsExist.getUser()
                ? userRepository.findById(cartItemsExist.getUser().getId()).orElse(new User())
                : new User();
    PosReceiptPdf posReceiptPdf = constructPosReceiptPdf(orderItems, user, reference, cartItemsExist.getNotes(),businessName);
    posReceiptPdf.setLogoPath(logoPath);
    posReceiptPdf.setSlogan(slogan);
    return pdfService.generatePOSReceiptPDF(posReceiptPdf);
  }
  public List<OrderItem> fetchShoppingCartItems(Long shoppingCartId) {
    ShoppingCart shoppingCart = shoppingCartRepository
      .findById(shoppingCartId)
      .orElseThrow(() -> new RuntimeException(SHOPPING_CART_NOT_EXIST));
    return orderItemRepository.findItemsByShoppingCartId(shoppingCart.getId());
  }
  private PosReceiptPdf constructPosReceiptPdf(List<OrderItem> orderItems,
                                               User user,
                                               String refernce,
                                               String nonUser,
                                               String businessName){
    List<CartDetail> cartDetails = new ArrayList<>();
    int totalQuantity = 0;
    double totalPrice = 0;
    for (OrderItem orderItem : orderItems) {
      if(Objects.nonNull(orderItem)){
        CartDetail cartItem = CartDetail.builder()
          .id(orderItem.getShopProduct().getProductCode())
          .productName(orderItem.getName())
          .price(orderItem.getPrice())
          .quantity(orderItem.getQty())
          .total((orderItem.getPrice() * orderItem.getQty()))
          .build();
        totalPrice += (orderItem.getPrice() * orderItem.getQty());
        totalQuantity += orderItem.getQty();
        cartDetails.add(cartItem);
      }
    }

    CartUserDetails cartUserDetails = constructCartUserDetails(user, refernce, nonUser,businessName);
    return PosReceiptPdf.builder().cartDetails(cartDetails)
      .cartUserDetails(cartUserDetails)
      .quantity(totalQuantity)
      .totalAmount(totalPrice)
      .build();
  }
  private CartUserDetails constructCartUserDetails(User user, String reference,
                                                   String nonUser,
                                                   String businessName){
    if(user.getId() == 0){
      return CartUserDetails.builder().name(StringUtils.isEmpty(nonUser) ? N_A : nonUser)
        .address(N_A)
        .contactNo(N_A)
        .company(N_A)
        .invoiceNo(reference)
        .build();
    }
    return CartUserDetails.builder().name(user.getFirstName().concat(" ").concat(user.getLastName()))
      .address(user.getAddress())
      .contactNo(user.getTelephone1())
      .company(businessName)
      .invoiceNo(reference)
      .build();
  }

  @Override
  public void generatePosReceiptPdf(long id, String htmlBuffer, HttpServletResponse response)
    throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    HtmlConverter.convertToPdf(htmlBuffer,byteArrayOutputStream);
    ServletOutputStream outputStream = response.getOutputStream();
    outputStream.write(byteArrayOutputStream.toByteArray());
    byteArrayOutputStream.close();
    outputStream.close();
  }

}
