package com.bitsvalley.micro.controllers;

import com.bitsvalley.micro.domain.Invoice;
import com.bitsvalley.micro.domain.InvoiceLineItemDetail;
import com.bitsvalley.micro.domain.User;
import com.bitsvalley.micro.repositories.InvoiceLineItemDetailRepository;
import com.bitsvalley.micro.repositories.InvoiceRepository;
import com.bitsvalley.micro.repositories.UserRepository;
import com.bitsvalley.micro.services.PdfService;
import com.bitsvalley.micro.webdomain.RuntimeSetting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Controller
public class InvoiceController extends SuperController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    InvoiceLineItemDetailRepository invoiceLineItemDetailRepository;

    @Autowired
    PdfService pdfService;



    @GetMapping(value = "/registerInvoice")
    public String registerInvoice(ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        if (user == null) {
            return "findCustomer";
        }

        getEmployeesInModel(model,user.getOrgId());
        Invoice invoice = new Invoice();
        model.put("invoice", invoice);
        getEmployeesInModel(model, user.getOrgId());
        return "invoice";
    }


    @GetMapping(value = "/invoiceDetails/{id}")
    public String invoiceDetails(@PathVariable("id") long id, ModelMap model) {
        Invoice invoice = invoiceRepository.findById(id).get();
        Collections.reverse(invoice.getInvoiceLineItemDetail());
        getEmployeesInModel(model,invoice.getOrgId());
        model.put("invoice", invoice);
        model.put("invoiceLineItemDetail", new InvoiceLineItemDetail());
        return "invoiceDetails";
    }



    @PostMapping(value = "/registerInvoiceForm")
    public String registerInvoice(@ModelAttribute("invoice") Invoice invoice, ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        User usernameExists = userRepository.findByUserNameAndOrgId(invoice.getUsername(), user.getOrgId());
        if( usernameExists == null ){
            model.put("errorInvoice"," Enter A Valid Username");
            getEmployeesInModel(model,user.getOrgId());
            model.put("invoice", invoice);
            return "invoice";

        }
        invoice.setBranchCode(user.getBranch().getCode());
        invoice.setCreatedBy(user.getUserName());
        invoice.setCreatedDate(new Date());
        invoice.setUser(user);
        invoice.setOrgId(user.getOrgId());
        invoiceRepository.save(invoice);
        model.put("invoiceAccountInfo", invoice.getUsername() +" - Invoice Successfully Created");
        return "invoice";
//        return showInvoices(model, invoice.getOrgId());
    }


    @PostMapping(value = "/registerInvoiceLineItemForm")
    public String registerInvoice(@ModelAttribute("InvoiceLineItemDetail") InvoiceLineItemDetail invoiceLineItemDetail, ModelMap model, HttpServletRequest request ) {
//        User user = userRepository.findByUserName(getLoggedInUserName());
        String invoiceId = request.getParameter("invoiceId");
        Invoice invoice = invoiceRepository.findById(new Long(invoiceId)).get();
        double total = invoiceLineItemDetail.getUnitPrice() * invoiceLineItemDetail.getQuantity();
        invoiceLineItemDetail.setTotal(total);
        invoiceLineItemDetail.setInvoice(invoice);
        invoice.getInvoiceLineItemDetail().add(invoiceLineItemDetail);
        invoiceLineItemDetailRepository.save(invoiceLineItemDetail);
        invoice.setTotalSum(invoice.getTotalSum()+total);
        invoiceRepository.save(invoice);
        return showInvoices(model, invoice.getOrgId());
    }


    @PostMapping(value = "/updateInvoiceNotesForm")
    public String updateInvoiceNotes( ModelMap model, HttpServletRequest request ) {
//        User user = userRepository.findByUserName(getLoggedInUserName());
        String invoiceId = request.getParameter("invoiceId");
        Invoice invoice = invoiceRepository.findById(Long.parseLong(invoiceId)).get();
        invoice.setNotes(request.getParameter("notes"));
        //TODO: status, Date, ...
        invoiceRepository.save(invoice);
        return showInvoices(model, invoice.getOrgId());
    }


    @GetMapping(value = "/deleteInvoiceLineItem/{id}")
    public String deleteGL(@PathVariable("id") long id, ModelMap model) {
        InvoiceLineItemDetail byId = invoiceLineItemDetailRepository.findById(id).get();
        Invoice invoice = byId.getInvoice();
        invoice.getInvoiceLineItemDetail().remove(byId);
        invoice.setTotalSum(invoice.getTotalSum() - byId.getTotal());
        invoiceRepository.save(invoice);
        invoiceLineItemDetailRepository.delete(byId);
        return showInvoices(model, invoice.getOrgId());
    }


    @PostMapping(value = "/reloadInvoice")
    public String showInvoices( ModelMap model, long orgId) {
        List<Invoice> invoices = invoiceRepository.findByOrgId(orgId);
        Collections.reverse(invoices);
        model.put("invoices", invoices);
        return "invoices";
    }


    @GetMapping(value = "/invoices")
    public String invoices( ModelMap model, HttpServletRequest request) {
        User user = userRepository.findByUserName(getLoggedInUserName());
        showInvoices(model, user.getOrgId());
        return "invoices";
    }

    @GetMapping(value = "/printInvoice/{id}")
    public void printInvoice(@PathVariable("id") long id, ModelMap model,
                                 HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Content-disposition", "attachment;filename=" + id + "_invoice.pdf");

        Invoice invoice = invoiceRepository.findById(new Long(id)).get();
        User customer = userRepository.findByUserNameAndOrgId(invoice.getUser().getUserName(), invoice.getOrgId());
        RuntimeSetting runtimeSetting = (RuntimeSetting) request.getSession().getAttribute("runtimeSettings");
        String htmlInput = pdfService.generatePDFInvoice(invoice, runtimeSetting, customer);
        generateByteOutputStream(response, htmlInput);
    }

}
