package br.com.luiscgomes.projetocafe

class Cliente(cpf: String, nome: String, telefone: String, endereco: String, instagram: String) {

    var cpf: String
    var nome: String
    var telefone: String
    var endereco: String
    var instagram: String
    var status: String

    init {
        this.cpf = cpf
        this.nome = nome
        this.telefone = telefone
        this.endereco = endereco
        this.instagram = instagram
        this.status = "Ativo"
    }
}