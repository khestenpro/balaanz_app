<!doctype html>
<html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <title>Test Template</title>
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
        <link href="https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700&display=swap" rel="stylesheet">
        <style>
            *, html, body, table, td, tr, p, h1, h2, h3, h4, h5, h6, span, b, i {
                font-family: 'Roboto', sans-serif;
                font-weight: 400;
                padding: 0px;
                margin: 0px;
            }
            p{
                font-size: 16px;
                line-height: 25px;
                font-weight: 400;
                color: #000000;
            }
            h2, h6{
                color: #000000;
            }
            table{
                width: 100%;
            }
            .blue-bird{
                width: 100%;
            }
            .wrapper {
                width: 750px; 
                height: auto;
            }
            .logo-image{
                background-color: #0c1c33;
                width: 750px;
                height: 200px;
                text-align: center;
                border-radius: 30px;
            }
            .heading-wrapper{
            width: 100%;
            }
            .content-heading{
                text-align: center;
            }
            .content-heading h2{
                font-size: 30px;
                line-height: 35px;
                padding-top: 40px;
            }
            .content-heading h6{
                font-size: 20px;
                padding: 10px 0 40px 0;
                font-weight: 300;
            }
            .about-company{
                padding-bottom: 30px;
            }
            .about-payment{
                padding-bottom: 50px;
            }
            .company-details p{
                padding-right: 20px;
            }
            .payment-image{
                width: 50%;
            }
            .company-image {
                width: 50%
            }
            .payment-details{
                padding-left: 20px;
                width: 50%;
            }
            .payment-details p{
                width: 100%;
            }
            .payment-details p a {
                color: #0c1c33;
            }
            .amenities-section{
                width: 100%;
                background-color: #ebf1f5;
                border-radius: 30px; 
            }
            .amenities-inner-section {
                padding-bottom: 25px; 
            }
            .amenities-heading h2{
                text-align: center;
                padding: 35px 0px 30px 0px;
                width: 100%;
            }
            .amenities-section ul li {
                font-size: 18px;
                line-height: 38px;
                font-weight: 400;
                padding: 0px 0px;
                color: #0c1c33;
            }
            .amenities-section .amenities-list {
                padding-left: 50px;
            }
            .footer{
                width: 100%;
                background-color:#0c1c33;
                border-radius: 30px;
                margin-top: 50px;
                padding: 30px;
            }
            .contact-details p{
                padding: 7px 0;
                color: #ffffff;
                line-height: 24px;
            }
            .contact-details p a{
                color: #ffffff;
                text-decoration: none;
            }   
            .contact-details p img{
                margin-right: 10px;
                color: #ffffff;  
                vertical-align: middle;
            }
            .copyright {
                padding-top: 30px;
            }
            .copyright span{
                text-align: center;
                color: #ffffff;
                width: 100%;
            }
        </style>
    </head> 
<body>
<table class="blue-bird">
    <tr>
        <td class="container" align="center"> <!-- Main Container -->
            <table class="wrapper">
                <tr>
                    <td >  <!-- Wrapper -->
                        <table>
                            <tr>
                                <td> <!-- Section 1 -->
                                    <table>
                                        <tr>
                                            <td class="logo-image"> <!-- Blue Background -->
                                                <img src="images/logo.png" alt="" width="" height=""/>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                        <table class="heading-wrapper">
                            <tr>
                                <td class="content-heading"> <!-- Section 2 -->
                                    <h2>${amount} frs CFA - Direct Deposit Made To Your Account </h2>
                                    <h3>${payeeNote}</h3>
                                    <h6>View you account statement to see transaction details.</h6>
                                </td>
                            </tr>
                        </table>
                        <table>
                            <tr>
                                <td class="about-company"> <!-- Section 3 -->
                                    <table>
                                        <tr>
                                            <td class="company-details" valing="top">
                                                <p>Bank account deposit made
                                                </p>
                                            </td>
                                            <td class="company-image" valing="top">
                                                <img src="images/img1.png" alt="" width="" height="" />
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                        <table>
                            <tr>
                                <td class="about-payment" > <!-- Section 4 -->
                                    <table>
                                        <tr>
                                            <td class="payment-image" valing="top">
                                                <img src="images/img2.png" alt="" width="" height="" />
                                            </td>
                                            <td class="payment-details" valign="top">
                                                <p>Make direct payments to your friends and businesses
                                                </p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                        <table class="amenities-section">
                            <tr>
                                <td class="amenities-inner-section"> <!-- Section 5 -->
                                    <table>
                                        <tr>
                                            <td colspan="2" class="amenities-heading content-heading"><h2>Credit Union Features</h2></td>
                                        </tr>
                                        <tr>
                                            <td class="amenities-list">
                                                <ul>
                                                    <li>Momo Transafers</li>
                                                    <li>Orange Money Transfers</li>
                                                    <li>Account Statements</li>
                                                    <li>Balaanz POS</li>
                                                    <li>Balaanz Payroll</li>
                                                </ul>
                                            </td>
                                            <td>
                                                <ul>
                                                     <li>Momo Transafers</li>
                                                                                                        <li>Orange Money Transfers</li>
                                                                                                        <li>Account Statements</li>
                                                                                                        <li>Balaanz POS</li>
                                                </ul>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                        <table class="footer">
                            <tr>
                                <td class="footer-wrapper"> <!-- Section 6 -->
                                    <table>
                                        <tr>
                                            <td class="contact-details">
                                                <p valing="middle"><img src="images/email-icon.png" valing="middle" alt="" width="" height=""> <a href="mailto:creditunion@gmail.com">royal-credit@gmail.com</a></p>
                                                <p valing="middle"><img src="images/globe-icon.png" valing="middle" alt="" width="" height=""> <a href="http://www.royal-credit-union.com" >http://www.royal-credit-union.com</a></p>
                                            </td>
                                            <td class="contact-details">
                                                <p valing="middle"><img src="images/phone-icon.png" valing="middle" alt="" width="" height=""> Office: <a href="tel:+237683265165">+237 683 26 5165</a></p>
                                                <p valing="middle"><img src="images/phone-icon.png" valing="middle" alt="" width="" height=""> Manager: <a href="tel:+237683265166">+237 683 265 166</a></p>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="copyright" colspan="2" align="center"><span>@ All Rights Reserved.</span></td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>