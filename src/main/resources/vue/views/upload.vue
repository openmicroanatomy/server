<template id="uploader">
    <div>
        <h1>Slide uploader</h1>

        <template v-if="resumable.support">
            <div id="dropTarget">Pudota leike tähän :)</div>
            <div id="browseButton">Etsi leike</div>

            <div id="resumable-progress" v-if="files">
                <table>
                    <tr>
                        <td width="100%">
                            <div class="progress-container">
                                <div class="progress-bar"></div>
                            </div>
                        </td>
                        <td class="progress-text" nowrap="nowrap"></td>
                        <td class="progress-pause" nowrap="nowrap">
                            <button v-on:click="resume()" id="upload-resume" v-if="!uploading">Start / resume upload</button>
                            <button v-on:click="pause()" id="upload-pause" v-if="uploading">Pause upload</button>
                            <button v-on:click="cancel()" id="upload-cancel" v-if="uploading">Cancel upload</button>
                        </td>
                    </tr>
                </table>
            </div>

            <ul id="resumable-list" v-if="files">
                <li v-for="item in files">
                    <p>Name: {{ item.file.name }}</p>
                    <p>Size: {{ item.file.size }}</p>
                    <p>Type: {{ item.file.type }}</p>
                </li>
            </ul>
        </template>

        <template v-else>
            <p class="ui-state-error-text">Uploader not available.</p>
        </template>
    </div>
</template>

<script>
    Vue.component("uploader", {
        template: "#uploader",
        methods: {
            resume: function(event) {
                this.uploading = true;
            },
            pause: function(event) {
                this.uploading = false;
            },
            cancel: function(event) {
                this.uploading = false;
            }
        },
        /*watch: {
            resumable: function(newValue, oldValue) {
                if (newValue.support) {
                    this.resumable.assignBrowse(document.getElementById('browseButton'));
                    this.resumable.assignDrop(document.getElementById('dropTarget'));
                } else {
                    this.resumable.assignBrowse(null);
                    this.resumable.assignDrop(null);
                }
            }
        },*/
        mounted: function() {
            this.resumable = new Resumable({
                target: '/api/v0/upload',
                simultaneousUploads: 4,
                testChunks: false,
                throttleProgressCallbacks: 1
            });

            var uploader = this;

            this.resumable.assignBrowse(document.getElementById('browseButton'));
            this.resumable.assignDrop(document.getElementById('dropTarget'));

            this.resumable.on('fileAdded', function(file) {
                uploader.files = resumable.files;
                //$('.resumable-progress, .resumable-list').show();
                //$('.resumable-progress .progress-resume-link').hide();
                //$('.resumable-progress .progress-pause-link').show();
                //$('.resumable-list').append('<li class="resumable-file-'+file.uniqueIdentifier+'">Uploading <span class="resumable-file-name"></span> <span class="resumable-file-progress"></span>');
                //$('.resumable-file-'+file.uniqueIdentifier+' .resumable-file-name').html(file.fileName);

                // resumable.upload();
            });

            this.resumable.on('pause', function(){
                $('.resumable-progress .progress-resume-link').show();
                $('.resumable-progress .progress-pause-link').hide();
            });

            this.resumable.on('complete', function(){
                $('.resumable-progress .progress-resume-link, .resumable-progress .progress-pause-link').hide();
            });

            this.resumable.on('fileSuccess', function(file,message){
                $('.resumable-file-'+file.uniqueIdentifier+' .resumable-file-progress').html('(completed)');
            });

            this.resumable.on('fileError', function(file, message){
                $('.resumable-file-'+file.uniqueIdentifier+' .resumable-file-progress').html('(file could not be uploaded: '+message+')');
            });

            this.resumable.on('fileProgress', function(file){
                $('.resumable-file-'+file.uniqueIdentifier+' .resumable-file-progress').html(Math.floor(file.progress()*100) + '%');
                $('.progress-bar').css({width:Math.floor(r.progress()*100) + '%'});
            });

            this.resumable.on('cancel', function(){
                $('.resumable-file-progress').html('canceled');
            });

            this.resumable.on('uploadStart', function(){
                $('.resumable-progress .progress-resume-link').hide();
                $('.resumable-progress .progress-pause-link').show();
            });
        },
        data: function () {
            return {
                resumable: null,
                uploading: null,
                files: {}
            }
        }
    });
</script>
