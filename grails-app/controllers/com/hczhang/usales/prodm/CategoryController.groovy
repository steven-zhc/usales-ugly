package com.hczhang.usales.prodm

class CategoryController {

    static scaffold = Category

    def index() { }

    def add() {
        ["categories": Category.list()]
    }

    def doAdd() {
        def cat = new Category(params)

        if (cat.validate()) {
            cat.save()
            redirect action: "search"
        } else {
            flash.message = "Error add Category"
            [category: cat]
        }
    }

    def search(SearchCommand cmd) {
        if (cmd.hasErrors()) {
            ["categories": Category.list()]
        } else {
            def cs = Category.where {
                name =~ "%${cmd.name}%"
            }.list()
            ["command": cmd, "categories": cs]
        }
    }
}

class SearchCommand {
    String name

    static constraints = {
        importFrom Category
    }
}
