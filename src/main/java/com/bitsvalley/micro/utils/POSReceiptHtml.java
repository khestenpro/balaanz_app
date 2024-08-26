
package com.bitsvalley.micro.utils;

/**
 * POSReceiptHtml
 * Store for all HTML tags, styles
 * for POS receipt html builder
 */
public class POSReceiptHtml {

    public static  String HTML= "<html>%s</html>";
    public static  String HEAD = "<head>%s</head>";
    public static  String STYLES = "<style>\r\n" + //
            "        * {\r\n" + //
            "            margin: 0px;\r\n" + //
            "            padding: 0px;\r\n" + //
            "            font-size: 14px;\r\n" + //
            "            font-family: Arial, Helvetica, sans-serif;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .highlighter {\r\n" + //
            "            background: #333333;\r\n" + //
            "            color: #ffffff;\r\n" + //
            "            padding: 5px;\r\n" + //
            "            font-size: 14px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-logo {\r\n" + //
            "            padding-bottom: 20px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        table {\r\n" + //
            "            border: none;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-container {\r\n" + //
            "            padding: 20px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-data-list>tr>td {\r\n" + //
            "            padding-bottom: 5px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-underline-value td {\r\n" + //
            "            border-bottom: 1px dashed #333333;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        label {\r\n" + //
            "            font-size: 14px;\r\n" + //
            "            color: #999999;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .highlighted-table {\r\n" + //
            "            padding-top: 20px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .highlighted-table table {\r\n" + //
            "            border-collapse: collapse;\r\n" + //
            "            border: 1px solid #000000;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .highlighted-table table tr td {\r\n" + //
            "            border-bottom: 1px solid #000000;\r\n" + //
            "            white-space: nowrap;\r\n" + //
            "            padding: 5px 10px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .highlighted-table-label {\r\n" + //
            "            text-align: right;\r\n" + //
            "            background: #333333;\r\n" + //
            "            color: #ffffff;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-table {\r\n" + //
            "            padding-top: 20px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-table table {\r\n" + //
            "            border-collapse: collapse;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-table table th {\r\n" + //
            "            background: #333333;\r\n" + //
            "            border: 1px solid #000000;\r\n" + //
            "            color: #ffffff;\r\n" + //
            "            padding: 5px 10px;\r\n" + //
            "            font-size: 14px;\r\n" + //
            "            font-weight: normal;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-table table td {\r\n" + //
            "            padding: 5px 10px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-table table .table-body-cell {\r\n" + //
            "            border-right: 1px solid #000000;\r\n" + //
            "            border-left: 1px solid #000000;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .table-footer-cell {\r\n" + //
            "            border-bottom: 1px solid #000000;\r\n" + //
            "            border-right: 1px solid #000000;\r\n" + //
            "            border-left: 1px solid #000000;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .table-footer-col {\r\n" + //
            "            border-bottom: 1px solid #000000;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-table table td.receipt-total-label {\r\n" + //
            "            text-align: right;\r\n" + //
            "            padding: 0px;\r\n" + //
            "        }\r\n" + //
            "\r\n" + //
            "        .receipt-table table td.receipt-total-value {\r\n" + //
            "            padding: 0px;\r\n" + //
            "        }\r\n" + //
            "    </style>";

    public static  String BODY = "<body>%s</body>";
    public static  String TBALE_LOGO_TD= "<td class=\"receipt-logo\" valign=\"top\" colspan=\"4\">%s</td>";
    public static  String LOGO_TABLE= "<table width=\"100%%\">%s</table>";
    public static  String LOGO_TABLE_SLOGAN = "<td align=\"center\">%s</td>";
    public static String LOGO_IMG = "<img align=\"center\" width=\"125px\" src=\"%s\" alt=\"Embedded Image\" />";
    public static String LOGO_SLOGAN = "<tr><td class=\"highlighter\" align=\"center\">%s</td></tr>";
    public static  String TD_VALIGN = "<td width=\"40%%\" valign=\"top\">%s</td>";
    public static  String TABLE_RECEIPT_DATA_LIST = "<table width=\"100%%\" class=\"receipt-data-list\">%s</table>";
    public static  String TABLE_TR = "<tr>%s</tr>";
    public static  String RECEIPT_TABLE_TD = "<td class=\"receipt-table\" colspan=\"3\">%s</td>";

    public static String ROW = "<tr>\n" +
      "                <td align=\"right\" valign=\"top\" width=\"30%%\"><label>%s</label></td>\n" +
      "                <td valign=\"top\" width=\"70%%\">\n" +
      "                  <table border=\"0\" class=\"receipt-underline-value\" width=\"100%%\">\n" +
      "                    <tr>\n" +
      "                      <td>\n" +
                                "%s" +
      "                      </td>\n" +
      "                    </tr>\n" +
      "                  </table>\n" +
      "                </td>\n" +
      "              </tr>";
    public static String RECEIPT_TABLE_HEADERS = "<tr>\n" +
      "                <th align=\"left\">Id</th>\n" +
      "                <th align=\"left\">Product Name</th>\n" +
      "                <th align=\"right\">Price</th>\n" +
      "                <th align=\"right\">Quantity</th>\n" +
      "                <th align=\"right\">Tax</th>\n" +
      "                <th align=\"right\">Total</th>\n" +
      "              </tr>";
    public static String RECEIPT_TABLE_DATA ="<td>%s</td>\n" +
      "                <td class=\"table-body-cell\">%s</td>\n" +
      "                <td align=\"right\" class=\"table-body-cell\">%s </td>\n" +
      "                <td align=\"right\" class=\"table-body-cell\">%s </td>\n" +
      "                <td align=\"right\" class=\"table-body-cell\">%s </td>\n" +
      "                <td align=\"right\">%s </td>";
    public static String RECEIPT_TABLE_DATA_FOOTER = "<tr>\n" +
      "                <td class=\"table-footer-col\"></td>\n" +
      "                <td class=\"table-footer-cell\"></td>\n" +
      "                <td class=\"table-footer-cell\"></td>\n" +
      "                <td class=\"table-footer-cell\"></td>\n" +
      "                <td class=\"table-footer-cell\"></td>\n" +
      "                <td class=\"table-footer-col\"></td>\n" +
      "              </tr>";
    public static String RECEIPT_TABLE_TOTALS = "<tr>\n" +
      "                <td colspan=\"4\"></td>\n" +
      "                <td class=\"receipt-total-label\">%s</td>\n" +
      "                <td class=\"receipt-total-value\">\n" +
      "                  <table border=\"0\" class=\"receipt-underline-value\" width=\"100%%\">\n" +
      "                    <tr>\n" +
      "                      <td align=\"right\">\n" +
      "                        %s\n" +
      "                      </td>\n" +
      "                    </tr>\n" +
      "                  </table>\n" +
      "                </td>\n" +
      "              </tr>";
    public static String FINAL_TABLE = "" +
      "<table border=\"0\" width=\"100%%\">\n" +
      "  <tr>\n" +
      "    <td class=\"receipt-container\">\n" +
      "      <table border=\"0\" width=\"100%%\">%s" +
      "      </table>\n" +
      "    </td>\n" +
      "  </tr>\n" +
      "</table>";
}