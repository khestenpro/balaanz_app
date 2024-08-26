package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.*;
import com.bitsvalley.micro.repositories.ProductCategoryRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.*;
import com.bitsvalley.micro.utils.BVMicroUtils;
import com.bitsvalley.micro.utils.CollectionRequestStatus;
import com.bitsvalley.micro.utils.FileWriter;
import com.bitsvalley.micro.webdomain.CollectionRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * @author Fru Chifen
 * 11.06.2021
 */
@Controller
@Slf4j
public class POSController extends SuperController {

    public static final String MESSAGE = " is not registered, enter valid username, if you wish to proceed with un-registered, please move to anonymous";
    public static final String SUCCESS_MESSAGE = " is registered, press below button to continue";
    @Autowired
    private ShopProductService shopProductService;

    @Autowired
    private ProductCategoryService productCategoryService;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private WarehouseLocationService warehouseLocationService;

    @Autowired
    CMRService cmrService;

    @Autowired
    private PdfService pdfService;
    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileWriter fileWriter;

    @GetMapping(value = "/posProducts")
    public String posproducts(ModelMap model, HttpServletRequest request) {
        return "posProducts";
    }

    @GetMapping(value = "/posCheckout")
    public String posCheckout(ModelMap model, HttpServletRequest request) {
        return "posCheckout";
    }


    @GetMapping(value = "/posConfirmPayment")
    public String posConfirmPayment(ModelMap model, HttpServletRequest request) {
        return "posConfirmPayment";
    }


    @GetMapping(value = "/showPosCategoryProducts/{id}")
    public String showPosCategoryProducts(@PathVariable("id") Long id, ModelMap model, HttpServletRequest request) {

        long orgID = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        ProductCategory productCategory = productCategoryRepository.findById(id).get();
        Iterable<ShopProduct> shopProducts = productCategoryService.findByOrgIdProductCategory(orgID, productCategory);
        model.put("shoppingCart", (ShoppingCart) request.getSession().getAttribute(BVMicroUtils.SHOPPING_CART));
        model.put("shopProducts", shopProducts);
        model.put("categories", productCategoryService.findAll(orgID));
        return "posCategoryProducts";
    }


    @GetMapping(value = "/removeFromCart/{id}")
    public String removeFromCart(@PathVariable("id") Long id, ModelMap model, HttpServletRequest request) {

        ShoppingCart shoppingCart = (ShoppingCart) request.getSession().getAttribute(BVMicroUtils.SHOPPING_CART);
        long orgID = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        ShopProduct shopProduct = shopProductService.findByIdAndOrg(id);
        shoppingCart = shopProductService.removeProductFromCart(shopProduct, shoppingCart);
//        Iterable<ShopProduct> shopProducts = productCategoryService.findByI(id);
        Iterable<ShopProduct> shopProducts = productCategoryService.findByOrgIdProductCategory(orgID, shopProduct.getProductCategory());

        request.getSession().setAttribute(BVMicroUtils.SHOPPING_CART, shoppingCart);
        model.put("shopProducts", shopProducts);
        model.put("categories", productCategoryService.findAll(orgID));

        return "posCategoryProducts";
    }


    @GetMapping(value = "/addToCart/{id}")
    public String addToCart(@PathVariable("id") Long id, ModelMap model, HttpServletRequest request) {

        ShoppingCart shoppingCart = (ShoppingCart) request.getSession().getAttribute(BVMicroUtils.SHOPPING_CART);
        if (shoppingCart == null) {
            shoppingCart = new ShoppingCart();
        }
        long orgID = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        ShopProduct shopProduct = shopProductService.findByIdAndOrg(id);
        shoppingCart = shopProductService.addProductToCart(shopProduct, shoppingCart);
//        Iterable<ShopProduct> shopProducts = productCategoryService.findByI(id);
        Iterable<ShopProduct> shopProducts = productCategoryService.findByOrgIdProductCategory(orgID, shopProduct.getProductCategory());

        request.getSession().setAttribute(BVMicroUtils.SHOPPING_CART, shoppingCart);
        model.put("shopProducts", shopProducts);
        model.put("categories", productCategoryService.findAll(orgID));

        return "posCategoryProducts";
    }

    @GetMapping(value = "/posCategoryProducts")
    public String posCategoryProducts(ModelMap model, HttpServletRequest request) {

        long org = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        Iterable<ShopProduct> shopProducts = shopProductService.findAll(org);

        ShoppingCart shoppingCart = (ShoppingCart) request.getSession().getAttribute(BVMicroUtils.SHOPPING_CART);
        if (shoppingCart == null) {
            shoppingCart = new ShoppingCart();
        }
        request.getSession().setAttribute(BVMicroUtils.SHOPPING_CART, shoppingCart);

        model.put("shopProducts", shopProducts);
        model.put("categories", productCategoryService.findAll(org));
        return "posCategoryProducts";
    }

    @PostMapping(value = "/registerShopProductForm")
    public String registerSavingForm(@ModelAttribute("shopProduct") ShopProduct shopProduct,
                                     @RequestParam("image1") MultipartFile image1,
                                     ModelMap model, HttpServletRequest request) {
        long org = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        String path = fileWriter.writingFile(image1).orElse((StringUtils.EMPTY));
        shopProduct.setImageUrl1(path);
        shopProduct.setOrgId(org);
        shopProductService.save(shopProduct);
        model.put("warehouses", warehouseLocationService.findAll(org));
        model.put("categories", productCategoryService.findAll(org));
        model.put("shopProduct", shopProduct);
        model.put("shopProductInfo", "Successfully created " + shopProduct.getName());
        return "posShopProduct";
    }

    @GetMapping(value = "/registerShopProduct")
    public String registerSaving(ModelMap model, HttpServletRequest request) {
        long org = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        if (!model.containsKey("shopProduct")) {
            model.put("shopProduct", new ShopProduct());
        }
        model.put("warehouses", warehouseLocationService.findAll(org));
        model.put("categories", productCategoryService.findAll(org));
        model.put("shopProducts", shopProductService.findAll(org));

        return "posShopProduct";
    }

    @PostMapping(value = "/updateShopProduct")
    public String updateShopProduct(
            @ModelAttribute("shopProduct") ShopProduct shopProduct,
            HttpServletRequest request,
            @RequestParam("image1") MultipartFile file,
            ModelMap model) {
        if (file != null && !file.isEmpty()) {
            String path = fileWriter.writingFile(file).orElse((StringUtils.EMPTY));
            shopProduct.setImageUrl1(path);
        }
        shopProductService.save(shopProduct);
        model.remove("shopProduct");
        return registerSaving(model, request);
    }

    @GetMapping("/editShopProduct/{id}")
    public String editShopProduct(@PathVariable Long id,
                                  HttpServletRequest request,
                                  ModelMap model) {
        log.info("editing shop product : {}", model);
        if (!model.containsKey("shopProduct")) {
            ShopProduct byIdAndOrg = shopProductService.findByIdAndOrg(id);
            model.put("shopProduct", byIdAndOrg);
            model.put("editingButtonName", "Update");
        }
        return registerSaving(model, request);
    }

    @PostMapping(value = "/registerWarehouseLocationForm")
    public String registerWarehouseLocationForm(@ModelAttribute("warehouseLocation") WarehouseLocation warehouseLocation,
                                                ModelMap model, HttpServletRequest request) {
        long org = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        warehouseLocation.setOrgId(org);
        warehouseLocationService.save(warehouseLocation);
        model.put("warehouses", warehouseLocationService.findAll(org));
        model.put("warehouseLocationInfo", "Successfully created " + warehouseLocation.getName());

        return "posWarehouseLocation";
    }

    @GetMapping(value = "/registerWarehouseLocation")
    public String registerWarehouseLocation(ModelMap model, HttpServletRequest request) {
        long org = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        model.put("warehouseLocation", new WarehouseLocation());
        model.put("warehouses", warehouseLocationService.findAll(org));
        return "posWarehouseLocation";
    }

    @PostMapping(value = "/registerProductCategoryForm")
    public String registerProductCategoryForm(@ModelAttribute("productCategory") ProductCategory productCategory,
                                              ModelMap model, HttpServletRequest request) {
        long org = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        model.put("productCategoryInfo", "Successfully created " + productCategory.getName());
        productCategory.setOrgId((Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG));
        productCategoryService.save(productCategory);
        model.put("productCategories", productCategoryService.findAll(org));
        return "productCategory";
    }

    @GetMapping(value = "/registerProductCategory")
    public String registerProductCategory(ModelMap model, HttpServletRequest request) {
        long org = (Long) request.getSession().getAttribute(BVMicroUtils.CURRENT_ORG);
        model.put("productCategory", new ProductCategory());
        model.put("productCategories", productCategoryService.findAll(org));
        return "productCategory";
    }


    public String receiveCurrentFromMomo(ModelMap model, HttpServletRequest request, String momoNumber) {
        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);
//        String amount = request.getParameter("momoAmount");
//        int momoAmount = Integer.parseInt(amount);
        String collectionPhoneNumer = momoNumber.replaceAll(" ", "");

        //TODO: Populate from model
        CollectionRequest collectionRequest = new CollectionRequest();
        collectionRequest.setAmount(new BigDecimal(10));
        collectionRequest.setSenderPhoneNumber(Long.parseLong(collectionPhoneNumer));
//        collectionRequest.setSenderMsisdnProvider();
        CollectionRequestStatus collectionRequestStatus = cmrService.sendMomoCollectionRequest(collectionRequest);
        // check balances and show confirmation page
        // or show inline error message from origin page

        return "userHomeCustomer";
    }


    @PostMapping(value = "/checkoutShoppingCart")
    public String checkoutShoppingCart(ModelMap model, HttpServletRequest request) {
        String username = request.getParameter("currentAccountUser");
        User user;
        HttpSession session = request.getSession();
        String anonymousUser = request.getParameter("anonymousUser");

        if (StringUtils.isNotEmpty(username)) {
            user = Optional.ofNullable(userRepository.findByUserName(username)).orElse(new User());
            Object confirmation = request.getSession().getAttribute("confirmation");
            if (null == confirmation || !confirmation.equals("done")) {
                model.addAttribute("isConfirmationNeeded", "y");
                if (user.getId() == 0) {
                    setModelAttributes(model, MESSAGE, "0", username);
                } else {
                    session.setAttribute("confirmation", "done");
                    setModelAttributes(model, SUCCESS_MESSAGE, "1", username);
                }
                return "ccPaymentOptions";
            }
        } else if (null != anonymousUser) {
            username = anonymousUser;
            user = Optional.ofNullable(userRepository.findByUserName(username)).orElse(new User());
        } else {
            setModelAttributes(model, MESSAGE, "0", username);
            return "ccPaymentOptions";
        }

        ShoppingCart shoppingCart = (ShoppingCart) request.getSession().getAttribute(BVMicroUtils.SHOPPING_CART);

        String currentAccount = request.getParameter("currentAccount");

        Double availableBalaance = shopProductService.getAvailableBalance(currentAccount);
//        shoppingCart = shopProductService.checkoutShoppingCartDA(shoppingCart);

        String momoCheckout = null;
        String cashCheckout = null;
//        User user = (User) request.getSession().getAttribute(BVMicroUtils.CUSTOMER_IN_USE);

        if (user.getId() != 0) {
            for (OrderItem orderItem : shoppingCart.getOrderItems()) {
                orderItem.setUser(user);
                orderItem.setCreatedBy(user);
            }
        } else {
            for (OrderItem orderItem : shoppingCart.getOrderItems()) {
                orderItem.setNotes(username);
            }
        }
        if (currentAccount == null) {
            momoCheckout = request.getParameter("moMoSenderNumber");
            shoppingCart = shopProductService.checkoutShoppingCartMoMo(shoppingCart, "237"+momoCheckout);
        } else if (momoCheckout == null) {
//                cashCheckout = request.getParameter("cashCheckout");
            // Cash
            shoppingCart = shopProductService.checkoutShoppingCartCash(shoppingCart);
        }

//        model.put("productCategory", new ProductCategory() );
        request.getSession().setAttribute(BVMicroUtils.SHOPPING_CART, new ShoppingCart());
        model.put("amount", shoppingCart.getTotal());
        session.removeAttribute("confirmation");
        return "ccPaymentResult";
    }

    private void setModelAttributes(ModelMap model, String confirmationMessage, String error, String username) {
        model.addAttribute("confirmationMessage",
                username.concat(confirmationMessage));
        model.addAttribute("primary", error);
        model.addAttribute("username", username);
    }


    @PostMapping(value = "/checkoutPayments")
    public String ccConfirmPayment(ModelMap model, HttpServletRequest request) {

        String moMoSenderNumber = request.getParameter("moMoSenderNumber");
        if (StringUtils.isNotEmpty(moMoSenderNumber)) {
            receiveCurrentFromMomo(model, request, moMoSenderNumber);
        }

        return "ccPaymentOptions";
    }

    @GetMapping(value = "/printposreceipt/{id}")
    public void printPosReceipt(@PathVariable("id") Long id,
                                ModelMap model, HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        response.setHeader("Content-disposition", "attachment;filename=" + id + "_pos_receipt.pdf");
        String htmlInput = shoppingCartService.getShoppingCartDetails(id, request);
        shoppingCartService.generatePosReceiptPdf(id, htmlInput, response);
        //generateByteOutputStream(response, htmlInput);
    }


}