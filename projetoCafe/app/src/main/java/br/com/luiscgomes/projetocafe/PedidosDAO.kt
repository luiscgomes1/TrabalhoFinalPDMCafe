package br.com.luiscgomes.projetocafe

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PedidosDAO {
    private val db = FirebaseFirestore.getInstance()
    private val itemPedidoDAO = ItensPedidosDAO()

    suspend fun inserirPedido(pedido: Pedido) {
        val pedidosCollection = db.collection("pedidos")
        val pedidoRef = pedidosCollection.document(pedido.idPedido)

        val data = hashMapOf(
            "data" to pedido.data,
            "cpfCliente" to pedido.cpfCliente
        )

        // Adicionar os dados do pedido
        pedidoRef.set(data).await()

        // Adicionar os itens do pedido como subcoleção
        pedido.itensPedido.forEach { itemPedido ->
            itemPedidoDAO.inserirItem(pedido.idPedido, itemPedido)
        }
    }

    suspend fun listarPedidos(): List<Pedido> {
        val pedidosCollection = db.collection("pedidos")
        val querySnapshot = pedidosCollection.get().await()

        val pedidos = querySnapshot.documents.map { doc ->
            val itens = itemPedidoDAO.listarItens(doc.id)
            val pedido = Pedido(
                doc.id, // ID do documento (pedido)
                doc.getString("data") ?: "",
                doc.getString("cpfCliente") ?: "",
                itens.toMutableList()
            )

            // Log detalhado de cada pedido
            Log.d("PedidosDAO", "Pedido ID: ${pedido.idPedido}, Data: ${pedido.data}, Cliente: ${pedido.cpfCliente}")

            pedido
        }

        return pedidos
    }

    suspend fun deletarPedido(idPedido: String) {
        val pedidoRef = db.collection("pedidos").document(idPedido)
        // Deletar os itens do pedido
        itemPedidoDAO.removerItensDoPedido(idPedido)
        // Deletar o pedido
        pedidoRef.delete().await()
    }

    suspend fun atualizarPedido(pedido: Pedido) {
        val pedidoRef = db.collection("pedidos").document(pedido.idPedido)

        val data = hashMapOf(
            "data" to pedido.data,
            "cpfCliente" to pedido.cpfCliente
        )

        // Atualizar dados do pedido
        pedidoRef.set(data).await()

        // Atualizar itens do pedido
        itemPedidoDAO.removerItensDoPedido(pedido.idPedido)
        pedido.itensPedido.forEach { itemPedido ->
            itemPedidoDAO.inserirItem(pedido.idPedido, itemPedido)
        }
    }

    // Função para remover todos os itens de um pedido
    suspend fun removerItensDoPedido(idPedido: String) {
        itemPedidoDAO.removerItensDoPedido(idPedido)
    }
}
