package com.bitsvalley.micro.services;

import com.bitsvalley.micro.domain.*;
//import com.bitsvalley.micro.domain.ShoppingCartOrder;
import com.bitsvalley.micro.repositories.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.utils.CollectionRequestStatus;
import com.bitsvalley.micro.utils.OrderStatus;
import com.bitsvalley.micro.webdomain.CollectionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * @author Fru Chifen
 * 25.02.2023
 */
@Service
public class ShopProductService extends SuperService {

    @Autowired
    private ShopProductRepository shopProductRepository;

    @Autowired
    private GeneralLedgerService generalLedgerService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private BranchService branchService;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CMRService cmrService;


    @Autowired
    private POSAccountTransactionRepository posAccountTransactionRepository;

    public Iterable<ShopProduct> findAll(long orgId) {
        Iterable<ShopProduct> all = shopProductRepository.findByOrgId(orgId);
        return all;
    }

    public ShopProduct findByIdAndOrg(long id) {
        ShopProduct shopProduct = shopProductRepository.findById(id).get();
        return shopProduct;
    }

    public ShoppingCart removeProductFromCart(ShopProduct shopProduct, ShoppingCart shoppingCart) {
        OrderItem orderItem = new OrderItem();
        for (OrderItem aOrderItem : shoppingCart.getOrderItems()) {
            if (aOrderItem.getShopProduct().getId() == shopProduct.getId()) {
                if (aOrderItem.getQty() == 1) {
                    orderItem = aOrderItem;
                    shoppingCart.setTotal(shoppingCart.getTotal() - aOrderItem.getPrice());
                } else {
                    aOrderItem.setQty(aOrderItem.getQty() - 1);
                    shoppingCart.setTotal(shoppingCart.getTotal() - aOrderItem.getPrice());
                }
                break;
            }

        }
        shoppingCart.getOrderItems().remove(orderItem);

        return shoppingCart;
    }


    public ShoppingCart addProductToCart(ShopProduct shopProduct, ShoppingCart shoppingCart) {
        shoppingCart.getOrderItems();
        OrderItem orderItem = new OrderItem();
        boolean found = false;
        for (OrderItem aOrderItem : shoppingCart.getOrderItems()) {
            if (aOrderItem.getShopProduct().getId() == shopProduct.getId()) {
                aOrderItem.setQty(aOrderItem.getQty() + 1);
                found = true;
                break;
            }
        }
        if (!found) {

            orderItem.setShopProduct(shopProduct);
            orderItem.setQty(1);
            orderItem.setName(shopProduct.getName());
            orderItem.setShopProduct(shopProduct);
            orderItem.setPrice(shopProduct.getUnitPrice());
            orderItem.setOrderStatus(OrderStatus.SELECTED);
            shoppingCart.getOrderItems().add(orderItem);

        }
        shoppingCart.setTotal(shoppingCart.getTotal() + shopProduct.getUnitPrice());
        return shoppingCart;
    }

    public void save(ShopProduct shopProduct) {
        shopProductRepository.save(shopProduct);
    }

    public ShoppingCart checkoutShoppingCartDA(ShoppingCart shoppingCart) {

        prepareCheckout(shoppingCart);
        generalLedgerService.updateGLAfterPosAccountTransaction(createNewPosTransaction(shoppingCart, null), BVMicroUtils.CURRENT_GL_3004);
        return shoppingCart;
    }


    public ShoppingCart checkoutShoppingCartCash(ShoppingCart shoppingCart) {

        prepareCheckout(shoppingCart);

        generalLedgerService.updateGLAfterPosAccountTransaction(createNewPosTransaction(shoppingCart, null), BVMicroUtils.CASH_GL_5001);
        return shoppingCart;
    }

    public ShoppingCart checkoutShoppingCartMoMo(ShoppingCart shoppingCart, String collectionPhoneNumer) {
        prepareCheckout(shoppingCart);


//        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
        double momoAmount = shoppingCart.getTotal();
//        String collectionPhoneNumber = user.getTelephone1().replaceAll(" ", "");

        //TODO: Populate from model
        CollectionRequest collectionRequest = CollectionRequest.builder()
                .amount(new BigDecimal(momoAmount))
                .senderPhoneNumber(Long.parseLong(collectionPhoneNumer.replaceAll(" ", "")))
                .build();

        CollectionRequestStatus collectionRequestStatus = cmrService.sendMomoCollectionRequest(collectionRequest);
//        if(collectionRequestStatus == "SUCCESSFULL")
        generalLedgerService.updateGLAfterPosAccountTransaction(createNewPosTransaction(shoppingCart, null), BVMicroUtils.CASH_GL_5001);
        return shoppingCart;
    }

    private void prepareCheckout(ShoppingCart shoppingCart) {
        List<OrderItem> orderItemList = new ArrayList<OrderItem>();
        List<ShopProduct> shopProductList = new ArrayList<ShopProduct>();

        double sumTotal = 0;
        for (OrderItem aItem : shoppingCart.getOrderItems()) {
            aItem.setOrderStatus(OrderStatus.PLACED);
            sumTotal = sumTotal + aItem.getPrice() * aItem.getQty();
            //aItem.setPrice(sumTotal);
            orderItemList.add(aItem);
            aItem.setShoppingCart(shoppingCart);
            ShopProduct shopProduct = aItem.getShopProduct();
            shopProduct.setStockAmount(shopProduct.getStockAmount() - aItem.getQty());
            shopProductList.add(shopProduct);
        }
        shoppingCartRepository.save(shoppingCart);
        orderItemRepository.saveAll(orderItemList);
        shoppingCart.setOrderItems(orderItemList);
        shoppingCart.setTotal(sumTotal);
        shopProductRepository.saveAll(shopProductList); //update inventory
    }

    public POSAccountTransaction createNewPosTransaction(ShoppingCart shoppingCart, PosAccount posAccount) {
        if (posAccount != null) {
//            PosAccount byAccountNumber = posAccountRepository.findByAccountNumberAndOrgId(accountNumber,orgId);
        }
        POSAccountTransaction posAccountTransaction = new POSAccountTransaction();
        posAccountTransaction.setPosAccount(null);
        posAccountTransaction.setWithdrawalDeposit(1);
        posAccountTransaction.setSumTotal(shoppingCart.getTotal());
        posAccountTransaction.setNotes("POS GL Account to transfer.  ");
        posAccountTransaction.setCreatedBy(getLoggedInUserName());
        posAccountTransaction.setReference(BVMicroUtils.getSaltString());
//        Date date = BVMicroUtils.formatDate(new Date(System.currentTimeMillis()));
        posAccountTransaction.setCreatedDate(LocalDateTime.now());
        posAccountTransaction.setModeOfPayment(BVMicroUtils.CASH);
        Branch branchInfo = branchService.getBranchInfo(getLoggedInUserName());
        posAccountTransaction.setBranch(branchInfo.getId());
        posAccountTransaction.setBranchCode(branchInfo.getCode());
        posAccountTransaction.setBranchCountry(branchInfo.getCountry());
//                    currentAccountTransaction.setAccountOwner(byAccountNumber.getUser().getLastName());
        posAccountTransaction.setOrgId(branchInfo.getOrgId());
        posAccountTransaction.setCurrentAmountInLetters("SYSTEM");
        posAccountTransactionRepository.save(posAccountTransaction);
//        if( byAccountNumber != null){
//            byAccountNumber.getCurrentAccountTransaction().add(posAccountTransaction);
//        }
//        currentAccountRepository.save(byAccountNumber);
//        updateGeneralLedger(posAccountTransaction, BVMicroUtils.POS_GL_3333, BVMicroUtils.CREDIT, posAccountTransaction.getSumTotal(), true);

        // MUST AVOID THIS WAY OF USING VALUES
        // TEMPORARY - MAPPING GENERAL LEDGER WITH SHOPPING CART BY THE BELOW LINE
        posAccountTransaction.setAccountOwner(String.valueOf(shoppingCart.getId()));
        return posAccountTransaction;
    }

    public double getAvailableBalance(String accountNumberUsername) {

        return 100000.0;
    }

}
