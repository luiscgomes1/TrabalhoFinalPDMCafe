package br.com.luiscgomes.projetocafe

class Produto(id_produto: String, descricao:String, tipoDoGrao: String, pontoDaTorra: String, valor: Double, blend: Boolean) {

    var id_produto: String
    var descricao: String
    var tipoDoGrao: String
    var pontoDaTorra: String
    var valor: Double
    var blend: Boolean

    init {
        this.id_produto = id_produto
        this.descricao = descricao
        this.tipoDoGrao = tipoDoGrao
        this.pontoDaTorra = pontoDaTorra
        this.valor = valor
        this.blend = blend
    }
}