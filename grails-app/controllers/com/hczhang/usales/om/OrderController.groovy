package com.hczhang.usales.om

import com.hczhang.usales.prodm.Product
import groovy.json.JsonOutput

class OrderController {

    static defaultAction = "list"

    def String getProductsJSON() {
        def list = []
        for (p in Product.list()) {
            list.add([id: p.id, name: p.name, price: p.listPrice])
        }

        def json = JsonOutput.toJson([list: list])
    }

    def add() {
        ["products": getProductsJSON()]
    }

    def save(OrderCommand cmd) {

        if (cmd.hasErrors()) {
            render view: "add", model: ["cmd": cmd, "products": getProductsJSON()]
        }

        def order = new Order(cmd.properties)
        order.dateCreated = Date.parse("MM/dd/yyyy", cmd.date)

        for (l in cmd.newItems) {
            def line = new OrderLine(
                    product: Product.get(l.pid),
                    quantity: l.quantity,
                    model: l.model,
                    note: l.note,
                    purchase: new LineBody(l.purchase.properties),
                    sell: new LineBody(l.sell.properties)
            )

            order.addToLines(line)
        }

        order.settle()

        if (order.save(flush: true)) {
            flash.message = "Added a new Order successful."
            redirect action: "show", id: order.id
            return
        }

        render view: "add", model: ["order": cmd, "products": getProductsJSON()]
    }

    def update(OrderCommand cmd) {

        Order order = Order.get(cmd.id)

        order.deliverFee = cmd.deliverFee ?: order.deliverFee
        order.trackingNo = cmd.trackingNo ?: order.trackingNo
        order.payment    = cmd.payment    ?: order.payment
        order.status     = cmd.status     ?: order.status
        order.note       = cmd.note       ?: order.note


        if (cmd.items) {
            def exists = (cmd.items - null).inject([:]) { acc, item -> acc << [(item.id): item] }

            // Remove
            def toDelete = []
            for (OrderLine item in order.lines) {
                if (!exists.containsKey(item.id)) {
                    toDelete << item
                }
            }

            for (OrderLine item in toDelete) {
                order.removeFromLines(item)
                item.delete()
            }

            // Update items
            order.lines.each { item ->
                def u = exists[item.id]
                item.quantity = u.quantity
                item.model = u.model
                item.note = u.note

                item.purchase.properties = exists[item.id].purchase.properties
                item.sell.properties = exists[item.id].sell.properties
            }

        }

        if (cmd.newItems) {
            // Add new items
            (cmd.newItems - null).each { l ->
                if (l.pid != null) {
                    def line = new OrderLine(
                            product: Product.get(l.pid),
                            quantity: l.quantity,
                            model: l.model,
                            note: l.note,
                            purchase: new LineBody(l.purchase.properties),
                            sell: new LineBody(l.sell.properties)
                    )
                    order.addToLines(line)
                }
            }
        }

        if (order.status < 4) {
            order.settle()
        }

        if (order.save(flush: true)) {
            redirect action: "show", id: cmd.id
            return
        }

        flash.message = "Update Error."
        redirect action: "show", id: cmd.id
    }

    def show() {
        Order o = Order.findById(params.id)
        if (o) {
            ["order": o, "products": getProductsJSON()]
        } else {
            flash.message = "Cannot find order."
            redirect action: "list"
        }
    }

    def list(SearchOrderCommand cmd) {

        if (cmd.status || cmd.prodName) {
            def list = Order.where {
                if (cmd.status) {
                    status == cmd.status
                }

                if (cmd.prodName) {
                    lines?.product.name =~ "%${cmd.prodName}%"
                }
            }

            ["cmd": cmd, "orders": list]
        } else {
            ["orders": Order.list()]
        }


    }
}

class LineBodyCommand {
    Float price
    Float tax
    Float shipping
    Float discount
}

class LineCommand {

    Long id
    String pid
    String model
    Integer quantity
    String note

    LineBodyCommand purchase
    LineBodyCommand sell

}

class OrderCommand {

    Long id
    String date
    Float deliverFee
    String trackingNo
    String note
    Float payment
    Integer status

    List<LineCommand> items
    List<LineCommand> newItems

}

class SearchOrderCommand {
    String prodName
    Integer status

    static constraints = {

    }
}
