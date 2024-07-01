package br.com.luiscgomes.projetocafe

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ItensPedidosDAO {
    private val db = FirebaseFirestore.getInstance()

    // Função para inserir um item de pedido
    suspend fun inserirItem(idPedido: String, itemPedido: ItemPedido) {
        val itensCollection = db.collection("pedidos").document(idPedido).collection("itens")
        val itemData = hashMapOf(
            "produtoId" to itemPedido.produtoId,
            "quantidade" to itemPedido.quantidade
        )
        itensCollection.add(itemData).await()
    }

    // Função para listar os itens de um pedido
    suspend fun listarItens(idPedido: String): List<ItemPedido> {
        val itensCollection = db.collection("pedidos").document(idPedido).collection("itens")
        val querySnapshot = itensCollection.get().await()

        return querySnapshot.documents.map { doc ->
            ItemPedido(
                doc.getString("produtoId") ?: "",
                doc.getLong("quantidade")?.toInt() ?: 0
            )
        }
    }

    // Função para atualizar um item de pedido
    suspend fun atualizarItem(idPedido: String, itemId: String, itemPedido: ItemPedido) {
        val itemRef = db.collection("pedidos").document(idPedido).collection("itens").document(itemId)
        val itemData = hashMapOf(
            "produtoId" to itemPedido.produtoId,
            "quantidade" to itemPedido.quantidade
        )
        itemRef.set(itemData).await()
    }

    // Função para remover um item de pedido
    suspend fun removerItem(idPedido: String, itemId: String) {
        val itemRef = db.collection("pedidos").document(idPedido).collection("itens").document(itemId)
        itemRef.delete().await()
    }

    // Função para remover todos os itens de um pedido
    suspend fun removerItensDoPedido(idPedido: String) {
        val itensCollection = db.collection("pedidos").document(idPedido).collection("itens")
        val snapshot = itensCollection.get().await()

        val deleteBatch = db.batch()
        for (doc in snapshot.documents) {
            deleteBatch.delete(doc.reference)
        }
        deleteBatch.commit().await()
    }
}
