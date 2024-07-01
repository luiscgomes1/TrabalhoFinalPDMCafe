package br.com.luiscgomes.projetocafe


class Pedido( idPedido: String, data: String, cpfCliente: String, itensPedido: MutableList<ItemPedido> = mutableListOf()) {
    var idPedido: String
    var data: String
    var cpfCliente: String
    var itensPedido : MutableList<ItemPedido> = mutableListOf()

    init {
        this.idPedido = idPedido
        this.data = data
        this.cpfCliente = cpfCliente
        this.itensPedido = itensPedido
    }
}

