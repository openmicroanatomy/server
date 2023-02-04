<template id="setup">
	<div>
		<h1>Welcome to OpenMicroanatomy</h1>
		<h2>Setup</h2>
		<p>Looks like your server is not setup yet. Create the first organization and administrator account to get started!</p>

		<form>
			<label for="organizationName">Organization name</label>
			<input type="text" id="organizationName" v-model="organizationName" placeholder="Organization name" />

			<hr />

			<label for="email">Email</label>
			<input type="email" id="email" v-model="email" placeholder="Email" />

			<label for="name">Name</label>
			<input type="text" id="name" v-model="name" placeholder="Name" />

			<label for="password">Password</label>
			<input type="password" id="password" v-model="password" placeholder="Password" />

			<label for="repeatPassword">Repeat password</label>
			<input type="password" id="repeatPassword" v-model="repeatPassword" placeholder="Repeat password" />

			<input type="button" value="Submit" v-on:click="submit"/>

			<hr>

			<ul>
				<li v-for="error in errors">
					{{ error }}
				</li>
			</ul>
		</form>
	</div>
</template>

<script>
	Vue.component("setup", {
		template: "#setup",
		data() {
			return {
				organizationName: "",
				email: "",
				name: "",
				password: "",
				repeatPassword: "",
				errors: []
			}
		},
		methods: {
			async submit() {
				const data = new FormData();
				data.set("organizationName", this.organizationName);
				data.set("email",            this.email);
				data.set("name",             this.name);
				data.set("password",         this.password);
				data.set("repeatPassword",   this.repeatPassword);

				const response = await fetch("/initialize", {
					body: data,
					method: "POST"
				});

				if (response.status === 200) {
					alert("Successfully initialized server -- reloading.")
					window.location.reload();
				} else {
					const json = await response.json();
					console.log(json);

					if (json.error) {
						this.errors = Array.of(json.error);
					}
				}
			}
		}
	});
</script>
