package br.com.luiscgomes.projetocafe

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.*
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientesScreen(navController: NavController, clientesDAO: ClientesDAO) {
    var clientes by remember { mutableStateOf(emptyList<Cliente>()) }
    var filteredClientes by remember { mutableStateOf(emptyList<Cliente>()) }
    var isFiltering by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var cpfBusca by remember { mutableStateOf(TextFieldValue()) }

    var cpf by remember { mutableStateOf(TextFieldValue()) }
    var nome by remember { mutableStateOf(TextFieldValue()) }
    var telefone by remember { mutableStateOf(TextFieldValue()) }
    var endereco by remember { mutableStateOf(TextFieldValue()) }
    var instagram by remember { mutableStateOf(TextFieldValue("@")) }

    var editingCliente by remember { mutableStateOf<Cliente?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmDialogType by remember { mutableStateOf("") }
    var clienteToBeDeleted by remember { mutableStateOf<Cliente?>(null) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        clientes = clientesDAO.listarClientes()
        filteredClientes = clientes
    }

    val listState = rememberLazyListState()

    fun clearFields() {
        cpf = TextFieldValue()
        nome = TextFieldValue()
        telefone = TextFieldValue()
        endereco = TextFieldValue()
        instagram = TextFieldValue("@")
        editingCliente = null
    }

    fun validateFields(): Boolean {
        return cpf.text.isNotBlank() &&
                nome.text.isNotBlank() &&
                telefone.text.isNotBlank() &&
                endereco.text.isNotBlank() &&
                nome.text.all { it.isLetter() || it.isWhitespace() || it in "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ" }
    }

    fun addOrUpdateCliente() {
        if (!validateFields()) {
            Toast.makeText(context, "Por favor, preencha todos os campos corretamente.", Toast.LENGTH_SHORT).show()
            return
        }

        showConfirmDialog = true
        confirmDialogType = if (editingCliente == null) "Adicionar" else "Salvar"
    }

    fun confirmAddOrUpdateCliente() {
        val cliente = Cliente(
            cpf = cpf.text.filter { it.isDigit() },
            nome = nome.text,
            telefone = telefone.text.filter { it.isDigit() },
            endereco = endereco.text,
            instagram = instagram.text
        )

        CoroutineScope(Dispatchers.IO).launch {
            if (editingCliente == null) {
                clientesDAO.inserirCliente(cliente)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Cliente adicionado", Toast.LENGTH_SHORT).show()
                }
            } else {
                clientesDAO.atualizarCliente(cliente)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Cliente atualizado", Toast.LENGTH_SHORT).show()
                }
            }
            clientes = clientesDAO.listarClientes()
            filteredClientes = clientes
            withContext(Dispatchers.Main) {
                clearFields()
            }
        }
        showConfirmDialog = false
    }

    fun removeCliente(cliente: Cliente) {
        clienteToBeDeleted = cliente
        showConfirmDialog = true
        confirmDialogType = "Excluir"
    }

    fun confirmRemoveCliente() {
        clienteToBeDeleted?.let { cliente ->
            CoroutineScope(Dispatchers.IO).launch {
                clientesDAO.deletarCliente(cliente.cpf)
                clientes = clientesDAO.listarClientes()
                filteredClientes = clientes
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Cliente excluído", Toast.LENGTH_SHORT).show()
                    if (editingCliente == cliente) {
                        clearFields()
                        editingCliente = null
                    }
                }
            }
        }
        showConfirmDialog = false
    }

    fun formatCPF(cpf: String): String {
        val cleaned = cpf.filter { it.isDigit() }
        return cleaned.replace(Regex("(\\d{3})(\\d{3})(\\d{3})(\\d{2})"), "$1.$2.$3-$4")
            .take(14)
    }

    fun formatTelefone(telefone: String): String {
        val cleaned = telefone.filter { it.isDigit() }
        return cleaned.replace(Regex("(\\d{2})(\\d{5})(\\d{4})"), "($1) $2-$3")
            .take(15)
    }

    fun applyFilter() {
        filteredClientes = clientes.filter { it.cpf == cpfBusca.text.filter { it.isDigit() } }
        isFiltering = true
        showSearchDialog = false
    }

    fun resetFilter() {
        filteredClientes = clientes
        isFiltering = false
        cpfBusca = TextFieldValue()
    }

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
                    text = "Tela de Clientes",
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
                        TextField(
                            value = cpf,
                            onValueChange = { textFieldValue ->
                                val newText = textFieldValue.text.filter { it.isDigit() }
                                val formattedText = formatCPF(newText)
                                val cursorPosition = if (formattedText.length > textFieldValue.text.length) {
                                    formattedText.length
                                } else {
                                    textFieldValue.selection.end + (formattedText.length - textFieldValue.text.length)
                                }
                                cpf = TextFieldValue(formattedText, TextRange(cursorPosition))
                            },
                            label = { Text("CPF") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = nome,
                            onValueChange = {
                                if (it.text.all { char -> char.isLetter() || char.isWhitespace() || char in "áéíóúÁÉÍÓÚãõÃÕâêîôûÂÊÎÔÛçÇ" }) {
                                    nome = it
                                }
                            },
                            label = { Text("Nome") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = telefone,
                            onValueChange = { textFieldValue ->
                                val newText = textFieldValue.text.filter { it.isDigit() }
                                val formattedText = formatTelefone(newText)
                                val cursorPosition = if (formattedText.length > textFieldValue.text.length) {
                                    formattedText.length
                                } else {
                                    textFieldValue.selection.end + (formattedText.length - textFieldValue.text.length)
                                }
                                telefone = TextFieldValue(formattedText, TextRange(cursorPosition))
                            },
                            label = { Text("Telefone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = endereco,
                            onValueChange = { endereco = it },
                            label = { Text("Endereço") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = instagram,
                            onValueChange = { instagram = it },
                            label = { Text("Instagram") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { addOrUpdateCliente() }) {
                                Text(if (editingCliente == null) "Adicionar Cliente" else "Salvar Edição")
                            }
                            Button(onClick = { clearFields() }) {
                                Text(if (editingCliente == null) "Limpar Campos" else "Cancelar Edição")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Text(
                            text = "Lista de Clientes",
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
                                    contentDescription = "Buscar Cliente"
                                )
                                Text("Buscar")
                            }
                            Button(onClick = { resetFilter() }, enabled = isFiltering) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Limpar Filtros"
                                )
                                Text("Todos")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(filteredClientes) { cliente ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("CPF: ${formatCPF(cliente.cpf)}")
                                Text("Nome: ${cliente.nome}")
                                Text("Telefone: ${formatTelefone(cliente.telefone)}")
                                Text("Endereço: ${cliente.endereco}")
                                Text("Instagram: ${cliente.instagram}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(onClick = {
                                        cpf = TextFieldValue(formatCPF(cliente.cpf))
                                        nome = TextFieldValue(cliente.nome)
                                        telefone = TextFieldValue(formatTelefone(cliente.telefone))
                                        endereco = TextFieldValue(cliente.endereco)
                                        instagram = TextFieldValue(cliente.instagram)
                                        editingCliente = cliente
                                        CoroutineScope(Dispatchers.Main).launch {
                                            listState.scrollToItem(0)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar Cliente"
                                        )
                                        Text("Editar")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(onClick = { removeCliente(cliente) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir Cliente"
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

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Buscar Cliente") },
            text = {
                Column {
                    TextField(
                        value = cpfBusca,
                        onValueChange = { cpfBusca = it },
                        label = { Text("CPF do Cliente") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { applyFilter() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar Cliente"
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

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("$confirmDialogType Cliente") },
            text = {
                Text("Você tem certeza que deseja $confirmDialogType este cliente?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (confirmDialogType) {
                            "Adicionar", "Salvar" -> confirmAddOrUpdateCliente()
                            "Excluir" -> confirmRemoveCliente()
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
}
