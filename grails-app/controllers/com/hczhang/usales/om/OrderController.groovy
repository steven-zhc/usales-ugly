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
            def line = new OrderLine(l.properties)
            line.product = Product.get(l.pid)

            order.addToLines(line)
        }

        if (order.save()) {
            flash.message = "Added a new Order successful."
            redirect action: "show", id: order.id
            return
        } else {
            render view: "add", model: ["order": order, "products": getProductsJSON()]
        }

    }

    def update(OrderCommand cmd) {
        if (cmd.hasErrors()) {
            flash.message = "Update Order Failed."
            redirect action: "show", id: cmd.id
            return
        }

        Order order = Order.get(cmd.id)

        order.deliverFee = cmd.deliverFee ?: order.deliverFee
        order.trackingNo = cmd.trackingNo ?: order.trackingNo
        order.payment    = cmd.payment    ?: order.payment
        order.status     = cmd.status     ?: order.status

        if (cmd.items || cmd.newItems) {
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
                item.properties = exists[item.id].properties
            }

            // Add new items
            cmd.newItems.each { item ->
                OrderLine line = new OrderLine(item.properties)
                line.product = Product.get(item.pid)
                order.addToLines(line)
            }
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

    def list() {
        ["orders": Order.list()]
    }

    def search(SearchOrderCommand cmd) {
        if (cmd.hasErrors()) {
            render view: "list", model: ["orders": Order.list()]
            return
        }

        def list = Product.where {
            name =~ "%${cmd.prodName}%"
        }.list().inject([]) { acc, prod ->
            acc << Order.withCriteria {
                lines {
                    eq("product", prod)
                }
            }
        }.flatten().toSet()

        render view: "list", model: ["cmd": cmd, "orders": list]

    }

}

class DetailsCommand {
    Float price
    Float tax
    Float shipping
    Float discount
    Float total
}

class LineCommand {

    Long id
    String pid
    String model
    Integer quantity
    Float discountPrice

    Float shippingFee
    Float lineTotal
    Float lineProfit
    String note

    DetailsCommand purchase
    DetailsCommand sell

    static constraints = {
    }

}

class OrderCommand {

    Long id
    String date
    Float deliverFee
    String trackingNo
    String note
    Float total
    Float profit
    Float payment
    Integer status

    List<LineCommand> items
    List<LineCommand> newItems

    static constraints = {
        // importFrom Order, exclude: ["id"]
    }

}

class SearchOrderCommand {
    String prodName

    static constraints = {

    }
}
