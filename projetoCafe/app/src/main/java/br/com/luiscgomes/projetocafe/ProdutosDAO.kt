package br.com.luiscgomes.projetocafe

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProdutosDAO {
    private val db = FirebaseFirestore.getInstance()
    private val produtosCollection = db.collection("produtos")

    suspend fun inserirProduto(produto: Produto) {
        val data = hashMapOf(
            "id_produto" to produto.id_produto,
            "descricao" to produto.descricao,
            "tipoDoGrao" to produto.tipoDoGrao,
            "pontoDaTorra" to produto.pontoDaTorra,
            "valor" to produto.valor,
            "blend" to produto.blend
        )
        produtosCollection.document(produto.id_produto).set(data).await()
    }

    suspend fun listarProdutos(): List<Produto> {
        val snapshot = produtosCollection.get().await()
        return snapshot.documents.map { doc ->
            Produto(
                doc.getString("id_produto")!!,
                doc.getString("descricao")!!,
                doc.getString("tipoDoGrao")!!,
                doc.getString("pontoDaTorra")!!,
                doc.getDouble("valor")!!,
                doc.getBoolean("blend")!!
            )
        }
    }

    suspend fun buscarProduto(id_produto: String): Produto? {
        val document = produtosCollection.document(id_produto).get().await()
        return document.toObject(Produto::class.java)
    }

    suspend fun deletarProduto(id_produto: String) {
        produtosCollection.document(id_produto).delete().await()
    }

    suspend fun atualizarProduto(produto: Produto) {
        val data = hashMapOf(
            "id_produto" to produto.id_produto,
            "descricao" to produto.descricao,
            "tipoDoGrao" to produto.tipoDoGrao,
            "pontoDaTorra" to produto.pontoDaTorra,
            "valor" to produto.valor,
            "blend" to produto.blend
        )
        produtosCollection.document(produto.id_produto).set(data).await()
    }
}