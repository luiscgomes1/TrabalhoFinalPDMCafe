package br.com.luiscgomes.projetocafe

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ClientesDAO {
    private val db = FirebaseFirestore.getInstance()
    private val clientesCollection = db.collection("clientes")

    suspend fun inserirCliente(cliente: Cliente) {
        withContext(Dispatchers.IO) {
            val data = hashMapOf(
                "cpf" to cliente.cpf,
                "nome" to cliente.nome,
                "telefone" to cliente.telefone,
                "endereco" to cliente.endereco,
                "instagram" to cliente.instagram,
                "status" to cliente.status
            )
            clientesCollection.document(cliente.cpf).set(data).await()
        }
    }

    suspend fun listarClientes(): List<Cliente> {
        return withContext(Dispatchers.IO) {
            val snapshot = clientesCollection.get().await()
            snapshot.documents.map { doc ->
                Cliente(
                    doc.getString("cpf")!!,
                    doc.getString("nome")!!,
                    doc.getString("telefone")!!,
                    doc.getString("endereco")!!,
                    doc.getString("instagram")!!,
                    doc.getString("status")!!
                )
            }
        }
    }

    suspend fun buscarCliente(cpf: String): Cliente? {
        return withContext(Dispatchers.IO) {
            val document = clientesCollection.document(cpf).get().await()
            document.toObject(Cliente::class.java)
        }
    }

    suspend fun deletarCliente(cpf: String) {
        withContext(Dispatchers.IO) {
            val clienteRef = clientesCollection.document(cpf)
            clienteRef.update("status", "Inativo").await()
        }
    }

    suspend fun atualizarCliente(cliente: Cliente) {
        withContext(Dispatchers.IO) {
            val data = hashMapOf(
                "cpf" to cliente.cpf,
                "nome" to cliente.nome,
                "telefone" to cliente.telefone,
                "endereco" to cliente.endereco,
                "instagram" to cliente.instagram
            )
            clientesCollection.document(cliente.cpf).set(data).await()
        }
    }
}
