package br.com.luiscgomes.projetocafe


class ItemPedido(produtoId: String, quantidade: Int) {
    var produtoId: String
    var quantidade: Int

    init {
        this.produtoId = produtoId
        this.quantidade = quantidade
    }
}