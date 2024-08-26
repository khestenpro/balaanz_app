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
                                                <img src="${imageLogo}" alt="" width="" height=""/>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                        <br/>
                        <table class="heading-wrapper">
                            <tr>
                                <td { text-align: left; }>
                                <h3>Hello ${firstname},</h3><br/>

                                    <h4>A ${transferType} notification from your Account at ${businessName}</h4>

                                    Please kindly contact your Financial Institution if you did not make this transfer, or have any questions concerning this transaction. <br/><br/>
                                    Sent to: ${phoneNumber} <br/>
                                    Amount <h2>${amount} ${currency}</h2><br/><br/>
                                    RequestId: ${requestId} <br/>
                                    Date/ Time: ${dateTime} <br/>

                                    <br/>
                                    Thank you,<br/>
                                    Your ${businessName} Online Team
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