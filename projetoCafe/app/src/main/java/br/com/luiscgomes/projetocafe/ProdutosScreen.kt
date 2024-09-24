package br.com.luiscgomes.projetocafe

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutosScreen(navController: NavController, produtosDAO: ProdutosDAO) {
    var produtos by remember { mutableStateOf(emptyList<Produto>()) }
    var filteredProdutos by remember { mutableStateOf(emptyList<Produto>()) }
    var isFiltering by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var idBusca by remember { mutableStateOf(TextFieldValue()) }

    var tipoDoGraoSelecionado by remember { mutableStateOf("") }
    var pontoDaTorraSelecionado by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf(TextFieldValue()) }
    var blend by remember { mutableStateOf(false) }
    var descricao by remember { mutableStateOf(TextFieldValue()) }

    var editingProduto by remember { mutableStateOf<Produto?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmDialogType by remember { mutableStateOf("") }
    var produtoToBeDeleted by remember { mutableStateOf<Produto?>(null) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        produtos = produtosDAO.listarProdutos()
        filteredProdutos = produtos
    }


    val tiposDeGrao = listOf("Arábica do Cerrado", "Conilon")
    val pontosDaTorra = listOf("Média", "Forte")

    fun clearFields() {
        tipoDoGraoSelecionado = ""
        pontoDaTorraSelecionado = ""
        valor = TextFieldValue()
        blend = false
        descricao = TextFieldValue()
        editingProduto = null

    }

    fun checkIDExists(id: String): Boolean {
        return produtos.any { it.id_produto == id }
    }


    fun generateNextId(): String {
        var nextId = (produtos.size + 1).toString()
        while (checkIDExists(nextId)) {
            nextId = (nextId.toInt() + 1).toString()
        }
        return nextId
    }

    val proximoIdProduto = generateNextId()


    fun validateFields(): Boolean {
        return tipoDoGraoSelecionado.isNotBlank() &&
                pontoDaTorraSelecionado.isNotBlank() &&
                valor.text.isNotBlank() &&
                descricao.text.isNotBlank() &&
                descricao.text.all { it.isLetter() || it.isWhitespace() || it in "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ" }
    }

    fun addOrUpdateProduto() {
        if (!validateFields()) {
            Toast.makeText(context, "Por favor, preencha todos os campos corretamente.", Toast.LENGTH_SHORT).show()
            return
        }

        showConfirmDialog = true
        confirmDialogType = if (editingProduto == null) "Adicionar" else "Salvar"
    }

    fun confirmAddOrUpdateProduto() {
        val valorDouble = valor.text.filter { it.isDigit() }.toDoubleOrNull()?.div(100) ?: 0.0

        val produto = Produto(
            id_produto = editingProduto?.id_produto ?: proximoIdProduto.toString(),
            tipoDoGrao = tipoDoGraoSelecionado,
            pontoDaTorra = pontoDaTorraSelecionado,
            valor = valorDouble,
            blend = blend,
            descricao = descricao.text
        )

        CoroutineScope(Dispatchers.IO).launch {
            if (editingProduto == null) {
                produtosDAO.inserirProduto(produto)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Produto adicionado", Toast.LENGTH_SHORT).show()
                }
            } else {
                produtosDAO.atualizarProduto(produto)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Produto atualizado", Toast.LENGTH_SHORT).show()
                }
            }
            produtos = produtosDAO.listarProdutos()
            filteredProdutos = produtos
            withContext(Dispatchers.Main) {
                clearFields()
            }
        }
        showConfirmDialog = false
    }

    fun confirmRemoveProduto() {
        produtoToBeDeleted?.let { produto ->
            CoroutineScope(Dispatchers.IO).launch {
                produtosDAO.deletarProduto(produto.id_produto)
                produtos = produtosDAO.listarProdutos()
                filteredProdutos = produtos
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Produto excluído", Toast.LENGTH_SHORT).show()
                    if (editingProduto == produto) {
                        clearFields()
                        editingProduto = null
                    }
                }
            }
        }
        showConfirmDialog = false
    }

    fun removeProduto(produto: Produto) {
        produtoToBeDeleted = produto
        showConfirmDialog = true
        confirmDialogType = "Excluir"
    }

    fun formatValor(valor: String): String {
        val cleanedValue = valor.filter { it.isDigit() }
        val number = cleanedValue.toDoubleOrNull() ?: return ""
        val decimalFormat = DecimalFormat("R$ #,##0.00")
        return decimalFormat.format(number / 100)
    }

    fun applyFilter() {
        filteredProdutos = produtos.filter { it.id_produto == idBusca.text }
        isFiltering = true
        showSearchDialog = false
    }

    fun resetFilter() {
        filteredProdutos = produtos
        isFiltering = false
        idBusca = TextFieldValue()
    }

    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                title = {
                    Text(
                        text = "Café Ouro Negro de Minas",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tela de Produtos",
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text("ID do Produto: ${editingProduto?.id_produto ?: proximoIdProduto}")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                                    TextField(
                                        value = descricao,
                                        onValueChange = {
                                            if (it.text.all { char -> char.isLetter() || char.isWhitespace() || char in "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ" }) {
                                                descricao = it
                                            }
                                        },
                                        label = { Text("Descrição/Nome do Produto:") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text("Tipo do Grão:")
                            tiposDeGrao.forEach { tipo ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = (tipo == tipoDoGraoSelecionado),
                                        onClick = { tipoDoGraoSelecionado = tipo }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = tipo)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text("Ponto da Torra:")
                            pontosDaTorra.forEach { ponto ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = (ponto == pontoDaTorraSelecionado),
                                        onClick = { pontoDaTorraSelecionado = ponto }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = ponto)
                                }
                            }
                          }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = valor,
                            onValueChange = { textFieldValue ->
                                val newText = textFieldValue.text.filter { it.isDigit() }
                                val formattedText = formatValor(newText)
                                val cursorPosition = if (formattedText.length > textFieldValue.text.length) {
                                    formattedText.length
                                } else {
                                    textFieldValue.selection.end + (formattedText.length - textFieldValue.text.length)
                                }
                                valor = TextFieldValue(formattedText, TextRange(cursorPosition))
                            },
                            label = { Text("Valor:") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text("Blend:")
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(checked = blend, onCheckedChange = { blend = it })
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { addOrUpdateProduto() }) {
                                Text(if (editingProduto == null) "Adicionar Produto" else "Salvar Edição")
                            }
                            Button(onClick = { clearFields() }) {
                                Text(if (editingProduto == null) "Limpar Campos" else "Cancelar Edição")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Text(
                            text = "Lista de Produtos",
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { showSearchDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar"
                                )
                                Text("Buscar")
                            }
                            Button(onClick = { resetFilter() }, enabled = isFiltering) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Todos"
                                )
                                Text("Todos")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(filteredProdutos) { produto ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("ID do Produto: ${produto.id_produto}")
                                Text("Descrição: ${produto.descricao}")
                                Text("Tipo do Grão: ${produto.tipoDoGrao}")
                                Text("Ponto da Torra: ${produto.pontoDaTorra}")
                                Text("Valor: R$ ${produto.valor}")
                                Text("Blend: ${if (produto.blend) "Sim" else "Não"}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(onClick = {
                                        descricao = TextFieldValue(produto.descricao)
                                        tipoDoGraoSelecionado = produto.tipoDoGrao
                                        pontoDaTorraSelecionado = produto.pontoDaTorra
                                        valor = TextFieldValue(produto.valor.toString().replace(".", "").replace(",", ""))
                                        blend = produto.blend
                                        editingProduto = produto
                                        CoroutineScope(Dispatchers.Main).launch {
                                            listState.scrollToItem(0)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar"
                                        )
                                        Text("Editar")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { removeProduto(produto) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir"
                                        )
                                        Text("Excluir")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("$confirmDialogType Produto") },
            text = {
                Text("Você tem certeza que deseja $confirmDialogType este produto?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (confirmDialogType) {
                            "Adicionar", "Salvar" -> confirmAddOrUpdateProduto()
                            "Excluir" -> confirmRemoveProduto()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmar"
                    )
                    Text("Confirmar")
                }
            },
            dismissButton = {
                Button(onClick = { showConfirmDialog = false }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar"
                    )
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Buscar Produto") },
            text = {
                Column {
                    TextField(
                        value = idBusca,
                        onValueChange = { idBusca = it },
                        label = { Text("ID do Produto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { applyFilter() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar"
                    )
                    Text("Buscar")
                }
            },
            dismissButton = {
                Button(onClick = { showSearchDialog = false }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar"
                    )
                    Text("Cancelar")
                }
            }
        )
    }
}