<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="layout" content="main" />
    <title>Order List</title>
</head>
<body>
    <h1>Order List</h1>
    <div>
        <form action="/order/search">
            <span>Product Name</span>
            <input type="text" name="prodName" value="${cmd?.prodName}"/>
            <button type="submit">Search</button>
        </form>
    </div>
    <div>
        <button type="button" onclick="location.href='/order/add'">Creat Order</button>
    </div>
    <div>
        <table>
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th>Product</th>
                    <th>Total</th>
                    <th>Profit</th>
                    <th>Payment</th>
                    <th>Note</th>
                </tr>
            </thead>
            <tbody>
                <g:each var="o" in="${orders}" status="i">
                <tr>
                    <td><a href="/order/show/${o.id}">${o.id}</a></td>
                    <td>${message(code: 'order.status.value.' + fieldValue(bean: o, field: "status"))}</td>
                    <td>${o.dateCreated.format('MM/dd/yyyy')}</td>
                    <td>
                        <g:each var="line" in="${o.lines}" status="j">
                            ${line.product.name}
                            <g:if test="${j != o.lines.size() - 1}">
                                <br/>
                            </g:if> 
                        </g:each>
                    </td>
                    <td>${o.total}</td>
                    <td>${o.profit}</td>
                    <td>${o.payment}</td>
                    <td>${o.note}</td>
                </tr>
                </g:each>
            </tbody>
        </table>
    </div>
</body>
</html>